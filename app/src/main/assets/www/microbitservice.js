/****

This JS file will provide the interface for touchdevelop. 

***/
var MicroBitService = (function () {
  var mInstance;

  //Services
  var mServices = {ALERT:0,FEEDBACK:1,INFORMATION:2,AUDIO:3,REMOTE:4,TELEPHONY:5,CAMERA:6,BUTTON:7};

  //Alert Commands
  var mAlertAction = {TOAST:0,VIBRATE:1,SOUND:2,RINGTONE:3,FINDPHONE:4};

  //Feedback Commands
  var mFeedbackAction = {DISPLAY:0};

  //Information Commands
  var mInformationAction = {ORIENTATION:0,SHAKE:1,BATTERY:2,TEMPERATURE:3,SIGNAL:4};

  //Audio Commands
  var mAudioAction = {STOP:0,START:1};

  //Remote Commands
  var mRemoteAction = {PLAY:0,PAUSE:1,STOP:2,NEXT_TRACK:3,PREV_TRACK:4,FORWARD:5,REWIND:6,VOLUME_UP:7,VOLUME_DOWN:8};

  //Button Commands
  var mButtonAction = {A_BUTTON_UP:0,A_BUTTON_DOWN:1,B_BUTTON_UP:2,B_BUTTON_DOWN:3,A_BUTTON_LONGPRESS:4,B_BUTTON_LONGPRESS:5,AB_BUTTON_PRESS:6};

  //Telephony Commands
  var mTelephonyAction = {CALL:0,SMS:1};

  //Camera Commands
  var mCameraAction = {CAMERA_PIC_MODE:0,CAMERA_VIDEO_MODE:1,TAKE_PIC:2,REC_VIDEO_START:3,REC_VIDEO_STOP:4};


  function init() {

    return {

    }
      command: function (service, action, value, callback) {
        console.log( "Requested service: " + service + " with action: " + action);

         PluginInterface.send(service, action, value, callback);
      },
      
      service: mServices,
      alert: mAlertAction,
      feedback: mFeedbackAction,
      information: mInformationAction,
      audio: mAudioAction,
      remote: mRemoteAction,
      telephony: mTelephonyAction,
      camera: mCameraAction
    };

  };

  return {

    getMBSInstance: function () {

      if ( !mInstance ) {
        mInstance = init();
      }

      return mInstance;
    }

  };

})()

var mbs = MicroBitService.getMBSInstance();
