import { localUrl } from '@renderer/utils'

export async function videoPage({ page = 1, pageSize = 1, name = '' }) {
  const  result = await window.electron.ipcRenderer.invoke('video/page', { page, pageSize, name })
  result.list  = (result.list || []).map(model=>{
    model.file_path = localUrl.addFileProtocol(model.file_path)
    return model
   })
   return result
}

export function findVideo(id) {
  return window.electron.ipcRenderer.invoke('video/find', id)
}

export function removeVideo(id) {
  return window.electron.ipcRenderer.invoke('video/remove', id)
}

export function saveVideo(video) {
  // id, model_id, name, text_content, voice_id, audio_path
  video.audio_path = localUrl.delFileProtocol(video.audio_path)
  return window.electron.ipcRenderer.invoke('video/save', video)
}

export function makeVideo(id) {
  return window.electron.ipcRenderer.invoke('video/make', id)
}

export function exportVideo(id, outputPath) {
  return window.electron.ipcRenderer.invoke('video/export', id, outputPath)
}

export function modifyVideo(video) {
  return window.electron.ipcRenderer.invoke('video/modify', video)
}

export function countVideo(name = '') {
  return window.electron.ipcRenderer.invoke('video/count', name)
}

export async function modelPage({ page = 1, pageSize = 1, name = '' }) {
  const result =  await window.electron.ipcRenderer.invoke('model/page', { page, pageSize, name })
   result.list  = (result.list || []).map(model=>{
    model.video_path = localUrl.addFileProtocol(model.video_path)
    return model
   })
   return result
}

export async function findModel(id) {
  const result = await window.electron.ipcRenderer.invoke('model/find', id)
  if(result){
    result.video_path = localUrl.addFileProtocol(result.video_path)
  }
  return result
}

export function addModel({ name, videoPath }) {
  videoPath = localUrl.delFileProtocol(videoPath)
  return window.electron.ipcRenderer.invoke('model/addModel', name, videoPath)
}

export function countModel(name = '') {
  return window.electron.ipcRenderer.invoke('model/count', name)
}

export function removeModel(id) {
  return window.electron.ipcRenderer.invoke('model/remove', id)
}

export function getContext(key) {
  return window.electron.ipcRenderer.invoke('context/get', key)
}

export function saveContext(key, val) {
  return window.electron.ipcRenderer.invoke('context/save', key, val)
}

export function audition(voiceId, text) {
  return window.electron.ipcRenderer.invoke('voice/audition', voiceId, text)
}
