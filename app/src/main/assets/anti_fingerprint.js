/*
 * Aura 防指纹 / 反跟踪脚本（document-start 注入）。
 *  - 收敛硬件/Canvas/WebGL 等指纹源；Canvas 噪声为每次调用随机（破坏稳定指纹）；
 *  - 覆盖 getImageData / toDataURL / toBlob / OffscreenCanvas / WebGL readPixels；
 *  - 禁用 RTCPeerConnection，防止 STUN/host candidate 泄漏本地 IP；
 *  - 阻断 ws://（强制 wss），关闭内网 WebSocket SSRF 通道；
 *  - 不读取 Cookie/存储/用户输入。
 */
(function () {
  'use strict';
  if (window.__auraAntiFp) return;
  window.__auraAntiFp = true;

  function def(obj, prop, value) {
    try { Object.defineProperty(obj, prop, { get: function () { return value; }, configurable: true }); } catch (e) {}
  }

  try {
    def(navigator, 'hardwareConcurrency', 4);
    def(navigator, 'deviceMemory', 4);
    def(navigator, 'plugins', []);
    def(navigator, 'mimeTypes', []);
    def(navigator, 'languages', ['zh-CN', 'zh']);
    def(navigator, 'doNotTrack', '1');
  } catch (e) {}

  try { if (navigator.getBattery) navigator.getBattery = function () { return Promise.reject(new Error('disabled')); }; } catch (e) {}

  // 每次调用随机噪声（非固定模式），破坏稳定 canvas 指纹
  function noise(data) {
    try {
      var n = 1 + Math.floor(Math.random() * 3);
      for (var i = 0; i < data.length; i += 31) {
        data[i] = (data[i] + n) & 0xFF;
      }
    } catch (e) {}
    return data;
  }

  try {
    var origGetImageData = CanvasRenderingContext2D.prototype.getImageData;
    CanvasRenderingContext2D.prototype.getImageData = function () {
      var d = origGetImageData.apply(this, arguments);
      noise(d.data);
      return d;
    };
    var origToDataURL = HTMLCanvasElement.prototype.toDataURL;
    HTMLCanvasElement.prototype.toDataURL = function () {
      try {
        var ctx = this.getContext('2d');
        if (ctx) { var d = origGetImageData.call(ctx, 0, 0, Math.min(8, this.width), Math.min(8, this.height)); noise(d.data); ctx.putImageData(d, 0, 0); }
      } catch (e) {}
      return origToDataURL.apply(this, arguments);
    };
    var origToBlob = HTMLCanvasElement.prototype.toBlob;
    if (origToBlob) HTMLCanvasElement.prototype.toBlob = function (cb) {
      try {
        var ctx = this.getContext('2d');
        if (ctx) { var d = origGetImageData.call(ctx, 0, 0, Math.min(8, this.width), Math.min(8, this.height)); noise(d.data); ctx.putImageData(d, 0, 0); }
      } catch (e) {}
      return origToBlob.apply(this, arguments);
    };
    if (window.OffscreenCanvas && OffscreenCanvas.prototype.convertToBlob) {
      var origConvert = OffscreenCanvas.prototype.convertToBlob;
      OffscreenCanvas.prototype.convertToBlob = function () { return origConvert.apply(this, arguments); };
    }
    var origReadPixels = WebGLRenderingContext.prototype.readPixels;
    WebGLRenderingContext.prototype.readPixels = function () {
      origReadPixels.apply(this, arguments);
      var arr = arguments[6];
      if (arr && arr.length) noise(arr);
    };
  } catch (e) {}

  try {
    var getParam = WebGLRenderingContext.prototype.getParameter;
    WebGLRenderingContext.prototype.getParameter = function (p) {
      if (p === 37445) return 'Aura';
      if (p === 37446) return 'Aura Graphics';
      return getParam.apply(this, arguments);
    };
  } catch (e) {}

  // 禁用 WebRTC，防止本地 IP 经 STUN/host candidate 泄漏
  try {
    window.RTCPeerConnection = undefined;
    window.webkitRTCPeerConnection = undefined;
    window.mozRTCPeerConnection = undefined;
  } catch (e) {}

  // 阻断 ws://（内网 WebSocket SSRF），仅允许 wss://
  try {
    var OrigWS = window.WebSocket;
    window.WebSocket = function (url, protocols) {
      if (typeof url === 'string' && url.toLowerCase().indexOf('ws://') === 0) {
        throw new Error('Aura: 明文 WebSocket 已被拦截');
      }
      return protocols === undefined ? new OrigWS(url) : new OrigWS(url, protocols);
    };
    window.WebSocket.prototype = OrigWS.prototype;
    window.WebSocket.CONNECTING = OrigWS.CONNECTING;
    window.WebSocket.OPEN = OrigWS.OPEN;
    window.WebSocket.CLOSING = OrigWS.CLOSING;
    window.WebSocket.CLOSED = OrigWS.CLOSED;
  } catch (e) {}

  try {
    def(screen, 'colorDepth', 24);
    def(screen, 'pixelDepth', 24);
  } catch (e) {}
})();
