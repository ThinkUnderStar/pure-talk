package thinkunderstar.puretalk.puretalkbackend.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import thinkunderstar.puretalk.puretalkbackend.common.DoSendComment;
import thinkunderstar.puretalk.puretalkbackend.common.Result;
import thinkunderstar.puretalk.puretalkbackend.entity.Comment;
import thinkunderstar.puretalk.puretalkbackend.entity.Post;
import thinkunderstar.puretalk.puretalkbackend.exception.BusinessException;
import thinkunderstar.puretalk.puretalkbackend.mapper.CommentMapper;
import thinkunderstar.puretalk.puretalkbackend.service.CommentService;
import thinkunderstar.puretalk.puretalkbackend.service.PostService;
import thinkunderstar.puretalk.puretalkbackend.service.SysCommentService;
import thinkunderstar.puretalk.puretalkbackend.util.RedisTokenBucketLimiter;
import thinkunderstar.puretalk.puretalkbackend.util.RedisUtils;

import java.util.concurrent.TimeUnit;

@Service
public class SysCommentServiceImpl implements SysCommentService {
    private final SensitiveWordManager sensitiveWordManager;
    private final CommentService commentService;
    private final RedisUtils redisUtils;
    private final CommentMapper commentMapper;
    private final PostService postService;
    private final RedisTokenBucketLimiter redisTokenBucketLimiter;

    public SysCommentServiceImpl(SensitiveWordManager sensitiveWordManager, CommentService commentService, RedisUtils redisUtils, CommentMapper commentMapper, PostService postService, RedisTokenBucketLimiter redisTokenBucketLimiter) {
        this.sensitiveWordManager = sensitiveWordManager;
        this.commentService = commentService;
        this.redisUtils = redisUtils;
        this.commentMapper = commentMapper;
        this.postService = postService;
        this.redisTokenBucketLimiter = redisTokenBucketLimiter;
    }

    @Override
    public Result sendComment(DoSendComment doSendComment) {
        if (doSendComment.getContent() == null || doSendComment.getContent().isEmpty()){
            throw new BusinessException("评论内容不能为空");
        }

        //用户限流
        boolean success = redisTokenBucketLimiter.tryAcquireByUser(String.valueOf(doSendComment.getUserId()), 5, 1);

        if (!success){
            throw new BusinessException("发送过于频繁");
        }

        if (sensitiveWordManager.checkSensitiveWord(doSendComment.getContent())){
            throw new BusinessException("文本中有敏感词汇");
        }

        Comment comment = new Comment(doSendComment.getPostId()
                , doSendComment.getUserId()
                , doSendComment.getContent()
                , doSendComment.getParentId()
                , doSendComment.getReplyUserId());

        commentService.save(comment);

        //评论数+1
        Post post = postService.getById(doSendComment.getPostId());
        post.setCommentCount(post.getCommentCount()+1);
        postService.updateById(post);

        return Result.success();
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Result deleteMyComment(long commentId) {
        Comment comment = commentService.getById(commentId);

        if (comment == null){
            throw new BusinessException("未查询到该评论");
        }

        if (comment.getUserId() != StpUtil.getLoginIdAsLong()){
            throw new BusinessException("不能删除他人的评论");
        }

        //删除用户自己发出的评论的业务代码
        commentService.remove(new LambdaQueryWrapper<Comment>().eq(Comment::getParentId, commentId));
        commentService.removeById(commentId);

        return Result.success();
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Result deleteComment(long commentId) {
        Comment comment = commentService.getById(commentId);

        if (comment == null){
            throw new BusinessException("未查询到该评论");
        }

        //删除帖子的业务代码
        commentService.remove(new QueryWrapper<Comment>().eq("parentId",commentId));
        commentService.removeById(commentId);

        return Result.success();
    }

    @Override
    public Result updateLikeCount(long commentId) {
        //用户限流
        boolean success = redisTokenBucketLimiter.tryAcquireByUser(StpUtil.getLoginIdAsString(), 10, 1);

        if (!success) {
            throw new BusinessException("点赞过于频繁");
        }

        Comment comment = commentService.getById(commentId);

        if (comment == null){
            throw new BusinessException("未查询到该评论");
        }

        String key = StpUtil.getLoginIdAsString() + ":isLikeComment:" + commentId;

        if (redisUtils.get(key) == null) {
            commentService.update(new LambdaUpdateWrapper<Comment>()
                    .eq(Comment::getId,commentId)
                    .setSql("like_count = like_count + 1"));

            redisUtils.set(key,"1",1, TimeUnit.DAYS);
        } else {
            commentService.update(new LambdaUpdateWrapper<Comment>()
                    .eq(Comment::getId,commentId)
                    .setSql("like_count = like_count - 1"));

            redisUtils.delete(key);
        }

        return Result.success();
    }

    @Override
    public Result getComments(long postId, int page, int size) {
        Page<Comment> commentPage = new Page<>(page,size);
        LambdaQueryWrapper<Comment> lambdaQueryWrapper = new LambdaQueryWrapper<Comment>()
                .eq(Comment::getPostId,postId)
                .orderByDesc(Comment::getCreateTime);

        Page<Comment> pageResult = commentMapper.selectPage(commentPage,lambdaQueryWrapper);

        return Result.success(pageResult);
    }
}
