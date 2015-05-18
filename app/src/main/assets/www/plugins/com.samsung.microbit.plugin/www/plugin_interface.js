cordova.define("com.samsung.microbit.plugin.PluginInterface", function(require, exports, module) { module.exports = {
  send: function(cmd, value, successCallback) {
    cordova.exec(successCallback,
                 function(err) {
                    console.log(err);
                 }, //failure callback
                 "PluginInterface",
                 "handleMessage",//action name
                 [cmd, value]);
  },
  init: function(JSCallback) {
    cordova.exec(JSCallback,
                 function(err) {
                    console.log(err);
                 }, //failure callback
                 "PluginInterface",
                 "setCallback",//action name
                 []);
  }
};

});
