cordova.define("com.samsung.microbit.plugin.PluginInterface", function(require, exports, module) { module.exports = {
  send: function(service, cmd, value, successCallback) {
    cordova.exec(successCallback,
                 function(err) {
                    console.log(err);
                 }, //failure callback
                 "PluginInterface",
                 "handleMessage",//action name
                 [service, cmd, value]);
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
