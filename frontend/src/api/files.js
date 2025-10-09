import axios from '@/utils/axios'

export function getFileMeta(id){
  return axios.get(`/api/files/${id}/meta`).then(r => r.data)
}

export function fetchFileBlob(id, mode = 'inline'){
  return axios.get(`/api/files/${id}/${mode}`, { responseType: 'blob' })
}
export function deleteFile(id){
  return axios.delete(`/api/files/${id}`).then(r => r.data)
}

