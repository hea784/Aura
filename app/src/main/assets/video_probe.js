/*
 * Aura 视频探测脚本 — 注入到用户添加的网站页面。
 * 职责：识别页面中的视频内容，判断短视频/普通视频形态，抓取 video 源地址，
 *       通过 AuraBridge 安全通道回传给原生；不修改页面行为、不自动播放。
 * 只上报元数据（数量/时长/尺寸/模式），绝不读取 Cookie、localStorage 或用户输入。
 */
(function () {
  'use strict';
  var BRIDGE = 'AuraBridge';
  var lastReport = '';

  function post(payload) {
    try {
      var serialized = JSON.stringify(payload);
      if (serialized === lastReport) return; // 去重，避免滚动时反复上报
      lastReport = serialized;
      if (window.AuraBridge && window.AuraBridge.postMessage) {
        window.AuraBridge.postMessage(serialized);
      }
    } catch (e) { /* 页面环境异常时静默 */ }
  }

  // 从 video 元素/network 记录中提取真实源
  function collectSources(video) {
    var sources = [];
    if (video.currentSrc) sources.push(video.currentSrc);
    if (video.src) sources.push(video.src);
    var children = video.querySelectorAll ? video.querySelectorAll('source') : [];
    for (var i = 0; i < children.length; i++) {
      if (children[i].src) sources.push(children[i].src);
    }
    return sources.filter(function (s) {
      return /^(https?:)?\/\//.test(s) || /\.m3u8|\.mp4|\.webm/.test(s);
    });
  }

  function scan() {
    var videos = document.querySelectorAll('video');
    var items = [];
    for (var i = 0; i < videos.length; i++) {
      var v = videos[i];
      var rect = v.getBoundingClientRect ? v.getBoundingClientRect() : { width: 0, height: 0 };
      var isVertical = rect.height > rect.width * 1.2 && rect.width > 0;
      var duration = isFinite(v.duration) ? v.duration : 0;
      items.push({
        type: 'video',
        index: i,
        duration: Math.round(duration),
        width: Math.round(rect.width),
        height: Math.round(rect.height),
        vertical: isVertical,
        // 短视频启发式：竖屏 或 时长 < 90 秒
        shortForm: isVertical || (duration > 0 && duration < 90),
        sources: collectSources(v).slice(0, 3)
      });
    }

    // 从 performance 资源记录补充 m3u8/mp4（覆盖 MSE/blob 播放场景）
    var streams = [];
    try {
      var entries = performance.getEntriesByType('resource') || [];
      for (var j = 0; j < entries.length; j++) {
        var url = entries[j].name || '';
        if (/\.m3u8(\?|$)/.test(url) || /\.mp4(\?|$)/.test(url)) {
          streams.push(url.split('?')[0]);
        }
      }
    } catch (e) { /* 忽略 */ }

    if (items.length === 0 && streams.length === 0) return;

    var hasShort = items.some(function (it) { return it.shortForm; });
    post({
      kind: 'aura-video-scan',
      count: items.length,
      suggestedMode: hasShort ? 'short' : 'normal',
      videos: items.slice(0, 20),
      streams: Array.from(new Set(streams)).slice(0, 10)
    });
  }

  // 页面加载后延迟扫描 + 懒加载视频用 MutationObserver 增量扫描
  function schedule() { setTimeout(scan, 1200); setTimeout(scan, 3500); }
  if (document.readyState === 'complete' || document.readyState === 'interactive') schedule();
  else window.addEventListener('DOMContentLoaded', schedule);

  try {
    var observer = new MutationObserver(function () { clearTimeout(window.__auraScanTimer); window.__auraScanTimer = setTimeout(scan, 1500); });
    observer.observe(document.documentElement || document.body, { childList: true, subtree: true });
  } catch (e) { /* 不支持则只做定时扫描 */ }
})();
