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
export function uploadAttachment(id, file, occurredAt){
  const fd = new FormData()
  fd.append('file', file)
  if (occurredAt) fd.append('occurredAt', occurredAt)
  return axios.post(`/api/demands/${id}/attachments`, fd).then(r => r.data)
}
export function deleteAttachment(attId) {
  return axios.delete(`/api/files/${attId}`).then(r => r.data)
}

