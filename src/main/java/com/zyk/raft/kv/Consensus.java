package com.zyk.raft.kv;

import com.zyk.raft.kv.entity.AppendEntryParam;
import com.zyk.raft.kv.entity.AppendEntryResult;
import com.zyk.raft.kv.entity.VoteParam;
import com.zyk.raft.kv.entity.VoteResult;

/**
 * Raft一致性模块
 */
public interface Consensus {
    /**
     * 请求投票
     * <p>
     * 接受者实现：
     * - 如果term < currentTerm 返回false
     * - 如果voteFor为空或者是candidateId，并且候选人的日志和自己一样新，就投票给他
     */
    VoteResult requestVote(VoteParam voteParam);

    /**
     * 追加日志
     *
     * 接收者实现：
     * - 如果term < currentTerm就返回false
     * - 如果日志在 prevLogIndex 位置处的日志条目任期号和 prevLogTerm 不匹配，则返回false
     * - 如果已经存在的日志条目和新的日志条目产生冲突（索引值相同但是任期号不同），删除这条之后的所有日志
     * - 附加任何在已有日志中不存在的条目
     * - 如果 leaderCommit > commitIndex 令 commitIndex 等于 leaderCommit 和 新日志条目索引值中较小的一个
     */
    AppendEntryResult appendEntries(AppendEntryParam appendEntryParam);

}
