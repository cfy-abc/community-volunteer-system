<template>
  <view class="comment-section">
    <view class="section-header">
      <text class="section-title">留言板 ({{ comments.length }})</text>
    </view>

    <!-- 评论输入框 -->
    <view class="comment-input-box" v-show="isLoggedIn">
      <textarea
        v-model="newComment"
        class="comment-textarea"
        placeholder="写下你的评论..."
        maxlength="500"
        :cursor-spacing="20"
        :adjust-position="true"
        :hold-keyboard="true"
        :fixed="true"
      />
      <button class="submit-btn" @tap="submitComment" :disabled="!newComment.trim() || submitting">
        {{ submitting ? '发送中...' : '发表评论' }}
      </button>
    </view>
    <view class="login-prompt" v-if="!isLoggedIn">
      <text>请先登录后再参与评论</text>
    </view>

    <!-- 评论列表 -->
    <view class="comment-list" v-if="comments.length > 0">
      <view class="comment-item" v-for="comment in comments" :key="comment.commentId">
        <view class="comment-avatar">
          <image :src="comment.avatar || '/static/images/default-avatar.png'" mode="aspectFill" />
        </view>
        <view class="comment-body">
          <view class="comment-header">
            <text class="comment-name">{{ comment.realName || comment.username }}</text>
            <text class="comment-tag" :class="'tag-' + comment.userTag">{{ tagLabel(comment.userTag) }}</text>
            <text class="comment-time">{{ formatTime(comment.createTime) }}</text>
          </view>
          <text class="comment-content">{{ comment.content }}</text>
          <view class="comment-actions">
            <text class="action-text" @tap="showReplyBox(comment.commentId)">
              回复 ({{ replyCount(comment.commentId) }})
            </text>
            <text class="action-text delete" v-if="canDelete(comment)" @tap="deleteComment(comment.commentId)">
              删除
            </text>
          </view>

          <!-- 回复输入框 -->
          <view class="reply-box" v-if="replyingTo === comment.commentId">
            <textarea
              v-model="replyContent"
              class="reply-textarea"
              placeholder="写下回复..."
              maxlength="300"
              :cursor-spacing="20"
              :adjust-position="true"
              :hold-keyboard="true"
            />
            <view class="reply-actions">
              <button class="reply-submit" size="mini" @tap="submitReply(comment.commentId)">回复</button>
              <button class="reply-cancel" size="mini" @tap="cancelReply">取消</button>
            </view>
          </view>

          <!-- 子回复列表 -->
          <view class="replies" v-if="comment.replies && comment.replies.length > 0">
            <view class="reply-item" v-for="reply in comment.replies" :key="reply.commentId">
              <text class="reply-name">{{ reply.realName || reply.username }}</text>
              <text class="reply-tag" :class="'tag-' + reply.userTag">{{ tagLabel(reply.userTag) }}</text>
              <text class="reply-text">{{ reply.content }}</text>
              <text class="reply-time">{{ formatTime(reply.createTime) }}</text>
            </view>
          </view>
        </view>
      </view>
    </view>

    <view class="empty-comments" v-else>
      <text>暂无评论，来发表第一条评论吧</text>
    </view>
  </view>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import request from '@/utils/request'

const props = defineProps({
  activityId: { type: [String, Number], required: true },
  isActivityCreator: { type: Boolean, default: false },
  hasParticipated: { type: Boolean, default: false }
})

const token = ref(uni.getStorageSync('token') || '')
const isLoggedIn = computed(() => !!token.value)
const comments = ref([])
const newComment = ref('')
const submitting = ref(false)
const replyingTo = ref(null)
const replyContent = ref('')

const userTag = computed(() => {
  if (!isLoggedIn.value) return 'unparticipated'
  if (props.isActivityCreator) return 'organizer'
  if (props.hasParticipated) return 'participated'
  return 'unparticipated'
})

const tagLabel = (tag) => {
  // Only three valid tags: organizer, participated, unparticipated
  // Map legacy/unknown tags (e.g. 'admin') to '参与' as safest fallback
  const map = {
    participated: '参与',
    unparticipated: '未参与',
    organizer: '发布方'
  }
  return map[tag] || map['unparticipated']
}

const replyCount = (commentId) => {
  const comment = comments.value.find(c => c.commentId === commentId)
  return comment?.replies?.length || 0
}

const canDelete = (comment) => {
  const userId = getCurrentUserId()
  return userId && comment.userId === userId
}

const base64Decode = (str) => {
  try {
    // 平台兼容的 base64 解码
    const chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/='
    let output = ''
    str = str.replace(/[^A-Za-z0-9\+\/\=]/g, '')
    for (let i = 0; i < str.length; i += 4) {
      const enc1 = chars.indexOf(str.charAt(i))
      const enc2 = chars.indexOf(str.charAt(i + 1))
      const enc3 = chars.indexOf(str.charAt(i + 2))
      const enc4 = chars.indexOf(str.charAt(i + 3))
      const chr1 = (enc1 << 2) | (enc2 >> 4)
      const chr2 = ((enc2 & 15) << 4) | (enc3 >> 2)
      const chr3 = ((enc3 & 3) << 6) | enc4
      output += String.fromCharCode(chr1)
      if (enc3 !== 64) output += String.fromCharCode(chr2)
      if (enc4 !== 64) output += String.fromCharCode(chr3)
    }
    return decodeURIComponent(escape(output))
  } catch (e) { return null }
}

const getCurrentUserId = () => {
  try {
    const tokenStr = uni.getStorageSync('token') || ''
    if (!tokenStr) return null
    const payload = tokenStr.split('.')[1]
    const decoded = JSON.parse(base64Decode(payload))
    return decoded.userId
  } catch (e) {
    return null
  }
}

const loadComments = async () => {
  try {
    const res = await request.get(`/api/activities/${props.activityId}/comments`)
    if (res.code === 200) {
      const list = res.data || []
      for (const comment of list) {
        const repliesRes = await request.get(`/api/activities/comments/${comment.commentId}/replies`)
        comment.replies = repliesRes.code === 200 ? (repliesRes.data || []) : []
      }
      comments.value = list
    }
  } catch (e) {
    console.error('加载评论失败:', e)
  }
}

const submitComment = async () => {
  if (!newComment.value.trim()) return
  submitting.value = true
  try {
    const res = await request.post(`/api/activities/${props.activityId}/comments`, {
      content: newComment.value.trim(),
      userTag: userTag.value
    })
    if (res.code === 200) {
      uni.showToast({ title: '评论成功', icon: 'success' })
      newComment.value = ''
      await loadComments()
    } else {
      uni.showToast({ title: res.msg || '评论失败', icon: 'none' })
    }
  } catch (e) {
    uni.showToast({ title: '网络错误', icon: 'none' })
  } finally {
    submitting.value = false
  }
}

const showReplyBox = (commentId) => {
  replyingTo.value = replyingTo.value === commentId ? null : commentId
  replyContent.value = ''
}

const cancelReply = () => {
  replyingTo.value = null
  replyContent.value = ''
}

const submitReply = async (commentId) => {
  if (!replyContent.value.trim()) return
  try {
    const res = await request.post(`/api/activities/comments/${commentId}/reply`, {
      content: replyContent.value.trim(),
      userTag: userTag.value
    })
    if (res.code === 200) {
      uni.showToast({ title: '回复成功', icon: 'success' })
      cancelReply()
      await loadComments()
    } else {
      uni.showToast({ title: res.msg || '回复失败', icon: 'none' })
    }
  } catch (e) {
    uni.showToast({ title: '网络错误', icon: 'none' })
  }
}

const deleteComment = (commentId) => {
  uni.showModal({
    title: '确认删除',
    content: '确定要删除这条评论吗？',
    success: async (res) => {
      if (!res.confirm) return
      try {
        const apiRes = await request.delete(`/api/activities/comments/${commentId}`)
        if (apiRes.code === 200) {
          uni.showToast({ title: '删除成功', icon: 'success' })
          await loadComments()
        } else {
          uni.showToast({ title: apiRes.msg || '删除失败', icon: 'none' })
        }
      } catch (e) {
        uni.showToast({ title: '网络错误', icon: 'none' })
      }
    }
  })
}

const formatTime = (timeStr) => {
  if (!timeStr) return ''
  const d = new Date(timeStr)
  const now = new Date()
  const diff = now - d
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return Math.floor(diff / 60000) + '分钟前'
  if (diff < 86400000) return Math.floor(diff / 3600000) + '小时前'
  return (d.getMonth() + 1) + '月' + d.getDate() + '日'
}

// Watch for activity change and load comments
watch(() => props.activityId, () => {
  if (props.activityId) loadComments()
}, { immediate: true })

// Also load on mount
import { onMounted } from 'vue'
onMounted(() => {
  if (props.activityId) loadComments()
})
</script>

<style lang="scss" scoped>
.comment-section {
  background: #fff;
  border-radius: 20rpx;
  padding: 30rpx;
  margin: 20rpx 30rpx;
}

.section-header {
  margin-bottom: 20rpx;
  .section-title {
    font-size: 32rpx;
    font-weight: bold;
    color: #333;
  }
}

.comment-input-box {
  display: flex;
  flex-direction: column;
  margin-bottom: 30rpx;
  .comment-textarea {
    width: 100%;
    min-height: 120rpx;
    border: 2rpx solid #e0e0e0;
    border-radius: 12rpx;
    padding: 20rpx;
    font-size: 28rpx;
    margin-bottom: 15rpx;
  }
  .submit-btn {
    align-self: flex-end;
    background: #667eea;
    color: #fff;
    border: none;
    border-radius: 30rpx;
    padding: 12rpx 30rpx;
    font-size: 26rpx;
  }
}

.login-prompt {
  text-align: center;
  padding: 30rpx;
  color: #999;
  font-size: 26rpx;
  margin-bottom: 20rpx;
}

.comment-item {
  display: flex;
  margin-bottom: 30rpx;
  .comment-avatar {
    width: 60rpx;
    height: 60rpx;
    border-radius: 50%;
    overflow: hidden;
    margin-right: 15rpx;
    flex-shrink: 0;
    image {
      width: 100%;
      height: 100%;
    }
  }
  .comment-body {
    flex: 1;
    .comment-header {
      display: flex;
      align-items: center;
      margin-bottom: 10rpx;
      flex-wrap: wrap;
      gap: 8rpx;
      .comment-name { font-size: 26rpx; color: #333; font-weight: 500; }
      .comment-tag {
        font-size: 20rpx;
        padding: 2rpx 10rpx;
        border-radius: 8rpx;
        &.tag-participated { background: #e8f5e8; color: #4caf50; }
        &.tag-unparticipated { background: #f5f5f5; color: #999; }
        &.tag-organizer { background: #fff3e0; color: #ff9800; }
      }
      .comment-time { font-size: 22rpx; color: #ccc; }
    }
    .comment-content {
      font-size: 28rpx;
      color: #333;
      line-height: 1.5;
      margin-bottom: 10rpx;
    }
    .comment-actions {
      display: flex;
      gap: 30rpx;
      .action-text {
        font-size: 22rpx;
        color: #999;
        &.delete { color: #f44336; }
      }
    }
  }
}

.reply-box {
  margin-top: 15rpx;
  .reply-textarea {
    width: 100%;
    min-height: 80rpx;
    border: 1rpx solid #e0e0e0;
    border-radius: 8rpx;
    padding: 10rpx;
    font-size: 26rpx;
  }
  .reply-actions {
    display: flex;
    gap: 10rpx;
    margin-top: 10rpx;
    justify-content: flex-end;
  }
  .reply-submit, .reply-cancel {
    font-size: 22rpx;
  }
  .reply-submit { background: #667eea; color: #fff; }
  .reply-cancel { background: #f0f0f0; color: #666; }
}

.replies {
  margin-top: 15rpx;
  padding-left: 20rpx;
  border-left: 2rpx solid #f0f0f0;
  .reply-item {
    margin-bottom: 15rpx;
    .reply-name { font-size: 24rpx; color: #667eea; font-weight: 500; }
    .reply-tag {
      font-size: 18rpx;
      padding: 2rpx 8rpx;
      border-radius: 6rpx;
      margin-left: 8rpx;
      &.tag-participated { background: #e8f5e8; color: #4caf50; }
      &.tag-unparticipated { background: #f5f5f5; color: #999; }
      &.tag-organizer { background: #fff3e0; color: #ff9800; }
    }
    .reply-text { font-size: 26rpx; color: #333; margin-left: 8rpx; }
    .reply-time { font-size: 20rpx; color: #ccc; margin-left: 10rpx; }
  }
}

.empty-comments {
  text-align: center;
  padding: 50rpx;
  color: #999;
  font-size: 26rpx;
}
</style>
