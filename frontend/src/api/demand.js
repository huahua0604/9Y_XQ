import axios from '@/utils/axios'

export function createDemand(payload){
  return axios.post('/api/demands', payload).then(r => r.data)
}
export function submitDemand(id){
  return axios.post(`/api/demands/${id}/submit`).then(r => r.data)
}
export function listMine(){
  return axios.get('/api/demands/mine').then(r => r.data)
}
export function listInbox(){
  return axios.get('/api/demands/inbox').then(r => r.data)
}
export function getDemand(id){
  return axios.get(`/api/demands/${id}`).then(r => r.data)
}
export function adminApprove(id, content){
  return axios.post(`/api/demands/${id}/admin/approve`, { content }).then(r => r.data)
}
export function adminReject(id, content){
  return axios.post(`/api/demands/${id}/admin/reject`, { content }).then(r => r.data)
}
export function reviewerApprove(id, content){
  return axios.post(`/api/demands/${id}/reviewer/approve`, { content }).then(r => r.data)
}
export function reviewerReject(id, content){
  return axios.post(`/api/demands/${id}/reviewer/reject`, { content }).then(r => r.data)
}
export function addComment(id, content){
  return axios.post(`/api/demands/${id}/comments`, { content }).then(r => r.data)
}
export function deleteComment(commentId) {
  return axios.delete(`/api/demands/comments/${commentId}`).then(r => r.data)
}
export function uploadAttachment(id, file, { note = '', occurredAt = '' } = {}) {
  const fd = new FormData()
  fd.append('file', file)
  if (occurredAt) fd.append('occurredAt', occurredAt)
  if (note) fd.append('note', note)
  return axios.post(`/api/demands/${id}/attachments`, fd, {
    headers: { 'Content-Type': 'multipart/form-data' }
  }).then(r => r.data)
}
export function deleteAttachment(attId) {
  return axios.delete(`/api/files/${attId}`).then(r => r.data)
}
export function markCompleted(id, note) {
  return axios.post(`/api/demands/${id}/complete`, note ? { note } : {}).then(r => r.data)
}
export function unmarkCompleted(id, note) {
  return axios.post(`/api/demands/${id}/complete/undo`, note ? { note } : {}).then(r => r.data)
}
export function markWeekDone(id, key, label) {
  return axios.post(`/api/demands/${id}/done/week`, { key, label }).then(r => r.data)
}
export function unmarkWeekDone(id, key, label) {
  return axios.post(`/api/demands/${id}/done/week/undo`, { key, label }).then(r => r.data)
}
export function markMonthDone(id, key, label) {
  return axios.post(`/api/demands/${id}/done/month`, { key, label }).then(r => r.data)
}
export function unmarkMonthDone(id, key, label) {
  return axios.post(`/api/demands/${id}/done/month/undo`, { key, label }).then(r => r.data)
}
export function updateDemandTitle(id, title) {
  return axios.put(`/api/demands/${id}/title`, { title }).then(r => r.data)
}
export function sendDemandSms(id, bizInfo, amount) {
  return axios.post(`/api/demands/${id}/sms/notify`, { bizInfo, amount }).then(r => r.data)
}