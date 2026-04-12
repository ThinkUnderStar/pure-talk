package thinkunderstar.puretalk.puretalkbackend.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import thinkunderstar.puretalk.puretalkbackend.common.DoSendPost;
import thinkunderstar.puretalk.puretalkbackend.common.Result;
import thinkunderstar.puretalk.puretalkbackend.entity.Comment;
import thinkunderstar.puretalk.puretalkbackend.entity.Post;
import thinkunderstar.puretalk.puretalkbackend.exception.BusinessException;
import thinkunderstar.puretalk.puretalkbackend.mapper.PostMapper;
import thinkunderstar.puretalk.puretalkbackend.service.CommentService;
import thinkunderstar.puretalk.puretalkbackend.service.PostService;
import thinkunderstar.puretalk.puretalkbackend.service.SysPostService;
import thinkunderstar.puretalk.puretalkbackend.util.RedisUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class SysPostServiceImpl implements SysPostService {
    private final long COMPREHENSIVE_SECTOR = 0;
    private final long HOTTEST_SECTOR = 1;
    private final long LATEST_SECTOR = 2;
    private static final BigDecimal WEIGHT_VIEW = BigDecimal.valueOf(1);
    private static final BigDecimal WEIGHT_LIKE = BigDecimal.valueOf(5);
    private static final BigDecimal WEIGHT_COMMENT = BigDecimal.valueOf(10);

    private final PostService postService;
    private final SensitiveWordManager sensitiveWordManager;
    private final CommentService commentService;
    private final RedisUtils redisUtils;
    private final PostMapper postMapper;

    public SysPostServiceImpl(PostService postService, SensitiveWordManager sensitiveWordManager, CommentService commentService, RedisUtils redisUtils, PostMapper postMapper) {
        this.postService = postService;
        this.sensitiveWordManager = sensitiveWordManager;
        this.commentService = commentService;
        this.redisUtils = redisUtils;
        this.postMapper = postMapper;
    }

    @Override
    public Result sendPost(DoSendPost doSendPost) {

        if (doSendPost.getContent() == null || doSendPost.getContent().isEmpty()){
            throw new BusinessException("不能发送空文本");
        }

        Post post = new Post();

        //敏感词审核
        if (sensitiveWordManager.checkSensitiveWord(doSendPost.getTitle())){
            throw new BusinessException("标题中含有敏感词");
        }

        if (sensitiveWordManager.checkSensitiveWord(doSendPost.getContent())){
            throw new BusinessException("文本中有敏感词汇");
        }

        post.setUserId(doSendPost.getUserId());
        post.setTitle(doSendPost.getTitle());
        post.setContent(doSendPost.getContent());

        postService.save(post);

        return Result.success();
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Result deleteMyPost(long postId) {
        Post post = postService.getById(postId);

        if (post == null){
            throw new BusinessException("未查询到该帖子");
        }

        if (post.getUserId() != StpUtil.getLoginIdAsLong()){
            throw new BusinessException("不能删除他人的帖子");
        }

        //删除用户自己发出的帖子的业务代码
        commentService.remove(new QueryWrapper<Comment>().eq("postId",postId));
        postService.removeById(postId);

        return Result.success();
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Result deletePost(long postId) {
        Post post = postService.getById(postId);

        if (post == null){
            throw new BusinessException("未查询到该帖子");
        }

        //删除帖子的业务代码
        commentService.remove(new QueryWrapper<Comment>().eq("postId",postId));
        postService.removeById(postId);

        return Result.success();
    }

    @Override
    public Result updateLikeCount(long postId) {

        Post post = postService.getById(postId);

        if (post == null){
            throw new BusinessException("未查询到该帖子");
        }

        String key = StpUtil.getLoginIdAsString() + ":isLikePost:" + postId;

        if (redisUtils.get(key) == null) {
            postService.update(new LambdaUpdateWrapper<Post>()
                    .eq(Post::getId,postId)
                    .setSql("like_count = like_count + 1"));

            redisUtils.set(key,"1",1, TimeUnit.DAYS);
        } else {
            postService.update(new LambdaUpdateWrapper<Post>()
                    .eq(Post::getId,postId)
                    .setSql("like_count = like_count - 1"));

            redisUtils.delete(key);
        }

        return Result.success();
    }

    @Override
    public Result updateViewCount(Map<Long, Integer> viewMap) {
        if (viewMap == null || viewMap.isEmpty()) {
            return Result.success();
        }

        LambdaUpdateWrapper<Post> updateWrapper = new LambdaUpdateWrapper<>();

        StringBuilder stringBuilder = new StringBuilder("view_count = CASE id ");
        List<Long> ids = new ArrayList<>();
        viewMap.forEach((k,v) -> {
            stringBuilder.append("WHEN ").append(k).append(" THEN view_count + ").append(v).append(" ");
            ids.add(k);
        });

        stringBuilder.append("END");

        updateWrapper.setSql(stringBuilder.toString())
                .in(Post::getId, ids);

        postService.update(updateWrapper);

        return Result.success();
    }

    @Override
    public void refreshSearchScore() {

        LambdaQueryWrapper<Post> queryWrapperWrapper = new LambdaQueryWrapper<>();
        queryWrapperWrapper.orderByDesc(Post::getUpdateTime);
        queryWrapperWrapper.last("LIMIT 5000");
        List<Post> list = postService.list(queryWrapperWrapper);

        if (list == null || list.isEmpty()){
            log.info("还没有可刷新查询系数的数据");

            return;
        }

        log.info("开始刷新最近活跃前5000的查询系数");

        list.forEach(post -> {
            BigDecimal viewCount = BigDecimal.valueOf(post.getViewCount());
            BigDecimal commentCount = BigDecimal.valueOf(post.getCommentCount());
            BigDecimal likeCount = BigDecimal.valueOf(post.getLikeCount());
            BigDecimal betweenHours = BigDecimal.valueOf(ChronoUnit.HOURS.between(post.getCreateTime(), LocalDateTime.now()));

            //计算综合查询系数
            post.setCompositeScore(viewCount
                    .add(commentCount.multiply(WEIGHT_COMMENT))
                    .add(likeCount.multiply(WEIGHT_LIKE))
                    .divide(betweenHours.add(BigDecimal.valueOf(1))
                            ,4
                            , RoundingMode.HALF_UP));

            //计算热度查询系数
            post.setHotScore(viewCount
                    .add(likeCount.multiply(WEIGHT_LIKE))
                    .add(commentCount.multiply(WEIGHT_COMMENT)));
        });

        postService.updateBatchById(list);

        log.info("刷新成功");
    }

    @Override
    public Result getPosts(long categoryId, int page, int size) {
        if (!(categoryId == 0||categoryId == 1||categoryId == 2)){
            throw new BusinessException("板块异常");
        }

        Page<Post> postPage = new Page<>(page,size);
        Page<Post> pageResult;

        if (categoryId == COMPREHENSIVE_SECTOR){
            //综合系数分页查询
            LambdaQueryWrapper<Post> lambdaQueryWrapper = (new LambdaQueryWrapper<Post>())
                    .orderByDesc(Post::getCompositeScore);
            pageResult = postMapper.selectPage(postPage, lambdaQueryWrapper);
        } else if (categoryId == HOTTEST_SECTOR) {
            //热度系数分页查询
            LambdaQueryWrapper<Post> lambdaQueryWrapper = (new LambdaQueryWrapper<Post>())
                    .orderByDesc(Post::getHotScore);
            pageResult = postMapper.selectPage(postPage, lambdaQueryWrapper);
        } else {
            //时间倒叙分页查询
            LambdaQueryWrapper<Post> lambdaQueryWrapper = (new LambdaQueryWrapper<Post>())
                    .orderByDesc(Post::getCreateTime);
            pageResult = postMapper.selectPage(postPage, lambdaQueryWrapper);
        }

        return Result.success(pageResult);
    }
}
