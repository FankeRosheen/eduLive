<template>
  <el-container>
    <el-main>
      <!-- <div id='teacher-video' ref="teachervideo"></div> -->
      <div v-for="list in liveList" :key="list.id">
        <div id="teacher-video" ref="teachervideo"></div>
      </div>
      <div id="init-video" ref="initteachervideo"></div>
    </el-main>
    <el-footer>
      <el-form :inline="true">
        <el-form-item>
          <el-button type="primary" @click="join()" id="join-btn" ref="joinBtn">加入</el-button>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="leave()" id="leave-btn" ref="leaveBtn">离开</el-button>
        </el-form-item>
        <el-form-item>
          <el-select v-model="selectedDefinition" @change="changeDefinition(selectedDefinition)" placeholder="清晰度选择">
            <el-option
              v-for="item in definition"
              :key="item.value"
              :label="item.label"
              :value="item.value">
              <span style="float: left">{{ item.label }}</span>
              <span style="float: right; color: #8492a6; font-size: 13px">{{ item.value }}</span>
            </el-option>
          </el-select>
        </el-form-item>
      </el-form>
    </el-footer>
  </el-container>
</template>

<script>
import {AgoraRTC} from '../assets/js/AgoraRTCSDK-2.1.1'

if (!AgoraRTC.checkSystemRequirements()) {
  alert('browser is no support webRTC')
}
AgoraRTC.Logger.error('this is error')
AgoraRTC.Logger.warning('this is warning')
AgoraRTC.Logger.info('this is info')
AgoraRTC.Logger.debug('this is debug')
let client, localStream, camera, microphone
// let mediaRecorder = new MediaStreamRecorder.WhammyRecorder()
export default {
  data () {
    return {
      liveList: [{id: 'id'}],
      definition: [{
        value: '720P',
        label: '超清'
      }, {
        value: '480P',
        label: '高清'
      }, {
        value: '270P',
        label: '标清'
      }],
      selectedDefinition: '',
      difineDefinition: '720p_3'
    }
  },
  methods: {
    getDevices: function getDevices () {
      AgoraRTC.getDevices(function (devices) {
        for (let i = 0; i !== devices.length; ++i) {
          let device = devices[i]
          if (device.kind === 'audioinput') {
            camera = device.id
            break
          }
        }
      })
    },
    changeDefinition: function(selectedDefinition) {
      let _this = this
      if(selectedDefinition === '480P'){
        _this.difineDefinition = '360P_8'
        _this.leave()
        _this.join()
        _this.$message.success("清晰度切换至高清（480P）")
      }else if(selectedDefinition === '270P'){
        _this.difineDefinition = '120P' //240P_3
        _this.leave()
        _this.join()
        _this.$message.success("清晰度切换至标清（270P）")
      }else{
        _this.difineDefinition = selectedDefinition
        _this.leave()
        _this.join()
        _this.$message.success("清晰度切换至超清（720P）")
      }
    },
    join: function () {
      let _this = this
      if(this.liveList.length === 0){
        this.liveList.push({id: 'teacher'})
      }
      //document.getElementById('teacher-video').style.visibility = 'visible'
      _this.$refs.initteachervideo.style.visibility = 'hidden'
      this.$refs.joinBtn.disabled = true
      if (this.$refs.leaveBtn.disabled === true) {
        this.$refs.leaveBtn.disabled = false
      }
      let appIDinput = 'a86334acf5c04a6aa7a85b66d0767612'
      // 此处理应将教师ID设为频道号，但是暂时无法调试，则固定
      let channelInput = '1000'
      // this.getDevices()
      client = AgoraRTC.createClient({mode: 'interop'})
      this.getDevices()
      client.init(appIDinput, function () {
        client.join(null, channelInput, null, function (uid) {
          microphone = 'default'
          localStream = AgoraRTC.createStream({
            streamID: uid,
            audio: true,
            cameraId: camera,
            microphoneId: microphone,
            video: true,
            screen: false
          })
          alert(_this.difineDefinition)
          localStream.setVideoProfile(_this.difineDefinition)
          // The user has granted access to the camera and mic.
          localStream.on('accessAllowed', function () {
          })
          // The user has denied access to the camera and mic.
          localStream.on('accessDenied', function () {
          })
          localStream.init(function () {
            localStream.play('teacher-video')
            client.publish(localStream, function (err) {
              alert('Publish stream failed' + err)
            })

            client.on('stream-published', function (evt) {
              _this.$message.success('发起直播成功！')
            })
          })
        })
      })
      let channelKey = ''
      client.on('error', function (err) {
        if (err.reason === 'DYNAMIC_KEY_TIMEOUT') {
          client.renewChannelKey(channelKey, function () {
          })
        }
      })
      client.on('stream-added', function (evt) {
        let stream = evt.stream
        client.subscribe(stream, function (err) {
          alert('Subscribe stream failed' + err)
        })
      })
      client.on('stream-subscribed', function (evt) {
      })
      client.on('stream-removed', function (evt) {
        let stream = evt.stream
        stream.stop()
        _this.$message.success('关闭摄像头成功')
      })
    },
    leave: function () {
      if(this.liveList.length > 0){
        alert('hhh')
        this.liveList.splice(0,1)
      }
      this.$refs.leaveBtn.disabled = true
      if (this.$refs.joinBtn.disabled === true) {
        this.$refs.joinBtn.disabled = false
      }
      let _this = this
      localStream.close()
      _this.$message.success('关闭摄像头成功')
      client.leave(function () {
      }, function (err) {
        alert(err)
      })
      client.unpublish(localStream, function (err) {
        console.log('Unpublish local stream failed' + err)
      })
      //document.getElementById('teacher-video').style.visibility = 'hidden'
      _this.$refs.initteachervideo.style.visibility = 'visible'
    }
  }
}
</script>

<style scoped>
  #teacher-video{
    margin-top: -30px;
    width: 380px;
    height: 200px;
    background-color: #94d2d0;
    position: absolute;
  }
  #init-video{
    margin-top: -30px;
    width: 380px;
    height: 200px;
    background-color: #94d2d0;
    position: absolute;
  }
  #join-btn{
    width: 100%;
  }
  #leave-btn{
    width: 100%;
  }
  .el-button{
    width: 100%;
  }
  .el-select{
    width: 130px;
  }
  .el-footer{
    margin-top: 170px;
  }
</style>
