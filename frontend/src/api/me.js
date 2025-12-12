import axios from '@/utils/axios'

export function fetchMyProfile() {
  return axios.get('/api/me').then(r => r.data)
}

export function updateMyProfile(payload) {
  return axios.put('/api/me', payload).then(r => ({ data: r.data, reauth: r.headers['x-reauth'] === 'true' }))
}

export function changeMyPassword(payload) {
  return axios.put('/api/me/password', payload).then(() => true)
}
