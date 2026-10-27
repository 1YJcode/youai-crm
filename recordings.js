// Loaded after app.js; recording bytes are fetched with the current user's token.
let closeCallRecording = null;
let customerTranscriptRequest = 0;
let closeCallIntelligence = null;

async function openCallIntelligence(segmentId,{afterConfirm}={}) {
  closeCallIntelligence?.();const account=state.auth.user?.id;
  const dialog=document.createElement('dialog');dialog.className='knowledge-dialog call-intelligence-dialog';
  dialog.setAttribute('aria-labelledby','callIntelligenceTitle');
  dialog.innerHTML=`<header><h2 id="callIntelligenceTitle">通话分析与跟进模板</h2><button type="button" class="button secondary" data-close>关闭</button></header>
    <p>DeepSeek 结果为待核对草稿。确认后保存为正式跟进，并归入该客户的私有知识档案。</p><p data-ai-state role="status">正在读取…</p>
    <section data-ai-results hidden><h3>本次通话摘要</h3><p data-ai-summary></p><h3>客户分析</h3><div data-customer-analysis></div><h3>销售改进建议</h3><div data-sales-coaching></div><h3>约定与提醒</h3><div data-reminders></div></section>
    <form><label>跟进模板<select name="template"></select></label><p data-template-reason></p><label>可修改的跟进内容<textarea name="content" maxlength="2000" rows="13" placeholder="分析完成后自动填入所选模板"></textarea></label><small data-content-count></small>
    <details><summary>查看所选模板的填充依据</summary><div data-field-evidence></div></details>
    <label><input type="checkbox" name="processingConsent">我同意将本次转写文字发送至 DeepSeek，生成客户分析、销售建议和模板草稿。</label>
    <div class="inline-actions"><button type="button" class="button secondary" data-generate>生成或重新分析</button><button class="button primary" type="submit" data-confirm disabled>确认并保存跟进</button></div><p data-save-state role="status"></p></form>`;
  const form=dialog.querySelector('form'),stateLabel=dialog.querySelector('[data-ai-state]'),message=dialog.querySelector('[data-save-state]');
  let closed=false,timer=null,current=null,chosen=null,confirming=false;const edits=new Map();
  const valid=()=>!closed&&state.auth.user?.id===account&&Boolean(state.auth.token);
  const cleanup=()=>{if(closed)return;closed=true;clearInterval(watcher);clearTimeout(timer);dialog.remove();closeCallIntelligence=null;};
  const watcher=setInterval(()=>{if(!valid())cleanup();},500);closeCallIntelligence=cleanup;
  dialog.querySelector('[data-close]').onclick=cleanup;dialog.onclose=cleanup;dialog.oncancel=cleanup;
  const count=()=>{dialog.querySelector('[data-content-count]').textContent=`${form.elements.content.value.length}/2000`;};
  function selectTemplate(name){
    if(chosen)edits.set(chosen,form.elements.content.value);chosen=name;
    const draft=current?.analysis?.templates?.find(t=>t.name===name);
    form.elements.content.value=current?.confirmedContent||edits.get(name)||draft?.content||'';count();
    dialog.querySelector('[data-field-evidence]').innerHTML=(draft?.fields||[]).map(f=>`<p><strong>${escapeHtml(f.label)}</strong>${escapeHtml(f.value)}${f.evidence?`<br><small>依据：${escapeHtml(f.evidence)}</small>`:''}</p>`).join('');
  }
  form.elements.template.onchange=()=>selectTemplate(form.elements.template.value);form.elements.content.oninput=()=>{if(chosen)edits.set(chosen,form.elements.content.value);count();};
  function findings(target,items){
    dialog.querySelector(target).innerHTML=(items||[]).map(item=>`<p><strong>${item.kind==='FACT'?'原文事实':item.kind==='AGREED'?'明确约定':'建议'}：</strong>${escapeHtml(item.text)}${item.evidence?`<br><small>原话依据：${escapeHtml(item.evidence)}</small>`:'<br><small>通用建议，需销售结合实际核对。</small>'}</p>`).join('')||'<p>未提及或暂无可确认内容。</p>';
  }
  async function load(){
    try{
      const data=await apiRequest(`/transcript-segments/${segmentId}/intelligence`);if(!valid())return;current=data;
      const labels={NOT_REQUESTED:'尚未生成分析草稿',WAITING_CONSENT:'等待本次分析授权',WAITING_CONFIG:'等待管理员配置 DeepSeek，录音和转写已保存',QUEUED:'分析已排队，请稍候…',PROCESSING:'DeepSeek 正在分析并预填模板…',COMPLETED:'分析完成，请销售核对、选择模板并修改',FAILED:'分析失败，可重试'};
      stateLabel.textContent=data.confirmedAt?`已由 ${data.confirmedByName} 确认保存`:(!data.configured&&!data.analysis?'DeepSeek 待管理员配置，录音和转写已保存':data.error||labels[data.state]||data.state);
      const analysis=data.analysis;dialog.querySelector('[data-ai-results]').hidden=!analysis;
      if(analysis){
        dialog.querySelector('[data-ai-summary]').textContent=analysis.summary;findings('[data-customer-analysis]',analysis.customerAnalysis);findings('[data-sales-coaching]',analysis.salesCoaching);findings('[data-reminders]',analysis.reminders);
        const selected=data.confirmedTemplate||chosen||analysis.recommendedTemplate;
        form.elements.template.innerHTML=analysis.templates.map(t=>`<option value="${escapeHtml(t.name)}">${escapeHtml(t.name)}${t.name===analysis.recommendedTemplate?'（推荐）':t.prefilled?'（已预填）':'（待补充）'}</option>`).join('');
        form.elements.template.value=selected;selectTemplate(selected);dialog.querySelector('[data-template-reason]').textContent=`推荐：${analysis.recommendedTemplate}。${analysis.recommendationReason||''}`;
      }
      const busy=['QUEUED','PROCESSING'].includes(data.state),confirmed=Boolean(data.confirmedAt);
      form.elements.content.readOnly=confirmed||!analysis;form.elements.template.disabled=confirmed||!analysis;
      dialog.querySelector('[data-confirm]').disabled=confirmed||busy||!analysis||data.state!=='COMPLETED';
      dialog.querySelector('[data-generate]').disabled=confirmed||busy||data.state==='WAITING_CONFIG';
      dialog.querySelector('[data-generate]').textContent=data.configured?'生成或重新分析':'授权并登记分析（待配置）';
      form.elements.processingConsent.disabled=confirmed||busy;
      clearTimeout(timer);if(busy)timer=setTimeout(load,2500);else if(data.state==='WAITING_CONFIG')timer=setTimeout(load,15000);
    }catch(error){if(valid())stateLabel.textContent=error.message;}
  }
  dialog.querySelector('[data-generate]').onclick=async event=>{
    if(!valid())return;if(!form.elements.processingConsent.checked){message.textContent='请先确认本次 DeepSeek 分析授权。';return;}
    event.currentTarget.disabled=true;
    try{await apiRequest(`/transcript-segments/${segmentId}/intelligence`,{method:'POST',body:JSON.stringify({processingConsent:true,preferredTemplate:chosen||null})});if(valid()){message.textContent='已登记分析请求';await load();}}
    catch(error){if(valid())message.textContent=error.message;}
    finally{if(valid()&&!['QUEUED','PROCESSING','WAITING_CONFIG'].includes(current?.state))event.currentTarget.disabled=false;}
  };
  form.onsubmit=async event=>{
    event.preventDefault();if(!valid()||confirming||!current?.analysis)return;
    const content=form.elements.content.value.trim();if(!content){message.textContent='请填写核对后的跟进内容。';return;}
    confirming=true;dialog.querySelector('[data-confirm]').disabled=true;
    try{
      const saved=await apiRequest(`/transcript-segments/${segmentId}/intelligence/confirm`,{method:'POST',body:JSON.stringify({version:current.version,template:form.elements.template.value,content})});
      if(!valid())return;current=saved;
      form.elements.content.readOnly=true;form.elements.template.disabled=true;form.elements.processingConsent.disabled=true;dialog.querySelector('[data-generate]').disabled=true;
      message.textContent='跟进已保存，并已归入客户私有知识档案。';
      if(saved.followUp){const task=normalizeTask(saved.followUp);const at=tasks.findIndex(t=>String(t.id)===String(task.id));if(at>=0)tasks[at]=task;else tasks.unshift(task);}
      await load();afterConfirm?.(saved);
      try{if(saved.followUp?.customerId){await refreshCustomerFollowUpCounts([saved.followUp.customerId]);if(state.customerDetailId===saved.followUp.customerId&&state.customerDetailTab==='follow')await loadCustomerFollowUps(state.customerDetailId);}}
      catch{toast('跟进已保存，刷新页面可查看最新记录。');}
    }catch(error){if(valid())message.textContent=error.message;}
    finally{confirming=false;if(valid()&&!current?.confirmedAt)dialog.querySelector('[data-confirm]').disabled=false;}
  };
  document.body.append(dialog);dialog.showModal();load();
}

function formatTranscriptDuration(seconds) {
  if(seconds==null || !Number.isFinite(Number(seconds)) || Number(seconds)<0)return '未记录';
  const value=Math.round(Number(seconds)),hours=Math.floor(value/3600),minutes=Math.floor(value%3600/60),rest=value%60;
  return `${hours?`${hours}小时`:''}${hours||minutes?`${minutes}分`:''}${rest}秒`;
}
function transcriptTimingMarkup(segment) {
  return `<dl class="transcript-source-timing"><div><dt>${segment.timeKind==='CALL'?'通话时间':'上传时间'}</dt><dd>${escapeHtml(formatDateTime(segment.sourceTime))}</dd></div><div><dt>录音通话时长</dt><dd>${formatTranscriptDuration(segment.durationSeconds)}</dd></div></dl>`;
}

async function loadCustomerTranscript(customerId,page=0) {
  const account=state.auth.user?.id,request=++customerTranscriptRequest;
  if(state.customerTranscriptCustomerId!==String(customerId) || state.customerTranscriptAccountId!==account) state.customerTranscriptSelection=[];
  state.customerTranscriptCustomerId=String(customerId);state.customerTranscriptAccountId=account;
  state.customerTranscript=null;state.customerTranscriptLoading=true;state.customerTranscriptError='';render();
  const valid=()=>request===customerTranscriptRequest && state.auth.user?.id===account && String(state.customerDetailId)===String(customerId) && state.customerDetailTab==='transcript';
  try {
    const data=await apiRequest(`/customers/${encodeURIComponent(customerId)}/transcript?page=${page}`);
    if(valid())state.customerTranscript=data;
  } catch(error){if(valid())state.customerTranscriptError=error.message;}
  finally{if(valid()){state.customerTranscriptLoading=false;render();}}
}
function customerTranscriptView(customer) {
  const valid=state.customerTranscriptCustomerId===String(customer.id) && state.customerTranscriptAccountId===state.auth.user?.id;
  const data=valid?state.customerTranscript:null,selected=valid?(state.customerTranscriptSelection||[]):[];
  const sourceLabel=s=>s==='SYSTEM'?'系统录音':s==='UPLOAD'?'上传录音':'历史录音';
  const page=data?.segments;
  return `<section class="data-panel customer-transcript"><div class="data-toolbar"><div><h2>客户转写档案</h2><p>每位客户一份档案，后续录音转写按来源分段追加。仅当前归属人和管理员可见。</p></div><button class="button secondary" data-transcript-refresh>刷新档案</button></div>
    ${state.customerTranscriptLoading?'<p role="status">正在读取转写档案…</p>':state.customerTranscriptError?`<p role="alert">${escapeHtml(state.customerTranscriptError)}</p>`:data?`
    <p>客户：${escapeHtml(data.customerName)}（${escapeHtml(data.customerNo)}） · 共 ${data.totalSegments} 段 · 已记录录音通话总时长：${formatTranscriptDuration(data.totalDurationSeconds)}${data.unknownDurationCount?` · ${data.unknownDurationCount} 段历史时长未记录`:''}</p>
    <div class="data-toolbar"><p>可选择多段内容整理公司案例；发布前仍需检查可共享内容。</p><button class="button secondary" data-transcript-case>整理所选段落（${selected.length}）</button></div>
    ${(page?.content||[]).map(s=>`<article class="transcript-segment"><header><label><input type="checkbox" data-transcript-select="${s.id}" ${selected.includes(s.id)?'checked':''}>选择此段</label></header>
      ${transcriptTimingMarkup(s)}
      <p>销售：${escapeHtml(s.salesName||'未记录')}${s.salesUserId!=null?`（账号 ${s.salesUserId}）`:''} · 客户：${escapeHtml(s.customerName)}（${escapeHtml(s.customerNo)}）</p>
      <p>来源：${sourceLabel(s.recordingSource)} · 通话 #${s.callId} · ${s.provider==='cloud'?'云端转写':s.provider==='local'?'本地转写':'历史转写'} · 转写完成时间：${s.transcribedAt?escapeHtml(formatDateTime(s.transcribedAt)):'历史时间未记录'}</p>
      <small>录音版本：${escapeHtml(s.recordingRevision)}</small><pre>${escapeHtml(s.transcript)}</pre><div class="inline-actions"><button class="button secondary" data-transcript-audio="${s.id}">回听来源录音</button><button class="button primary" data-transcript-intelligence="${s.id}">AI分析与跟进模板</button></div></article>`).join('')||'<p>暂无已完成的转写。上传此客户的录音并完成转写后，内容会自动追加到这里。</p>'}
    ${page?.totalPages>1?`<footer class="call-pagination"><button data-transcript-page="${page.number-1}" ${page.number===0?'disabled':''}>上一页</button><span>${page.number+1} / ${page.totalPages}</span><button data-transcript-page="${page.number+1}" ${page.number+1>=page.totalPages?'disabled':''}>下一页</button></footer>`:''}`:''}</section>`;
}
async function playTranscriptSegment(segment) {
  closeCallRecording?.();const account=state.auth.user?.id;
  const dialog=document.createElement('dialog');dialog.className='recording-dialog';
  dialog.innerHTML='<header><h2>来源录音与转写</h2><button class="button secondary" data-close>关闭</button></header><p data-source-meta></p><audio controls hidden></audio><p data-status role="status">正在读取来源录音…</p><details data-source-text hidden><summary>查看来源转写原文</summary><pre></pre></details>';
  if(segment.transcript){dialog.querySelector('[data-source-text]').hidden=false;dialog.querySelector('[data-source-text] pre').textContent=segment.transcript;}
  dialog.querySelector('[data-source-meta]').textContent=`${segment.customerName} · 销售：${segment.salesName} · 通话 #${segment.callId} · 录音版本 ${segment.recordingRevision}`;
  let closed=false,url=null;const audio=dialog.querySelector('audio');
  const valid=()=>!closed && state.auth.user?.id===account && Boolean(state.auth.token);
  const cleanup=()=>{if(closed)return;closed=true;clearInterval(watcher);audio.pause();audio.removeAttribute('src');audio.load();if(url)URL.revokeObjectURL(url);dialog.remove();closeCallRecording=null;};
  const watcher=setInterval(()=>{if(!valid())cleanup();},500);closeCallRecording=cleanup;
  dialog.querySelector('[data-close]').onclick=cleanup;dialog.oncancel=cleanup;dialog.onclose=cleanup;
  document.body.append(dialog);dialog.showModal();
  try {
    const blob=await apiRequest(`/calls/${segment.callId}/recording?revision=${encodeURIComponent(segment.recordingRevision)}`,{responseType:'blob',timeoutMs:60000});
    if(!valid())return;url=URL.createObjectURL(blob);audio.src=url;audio.hidden=false;dialog.querySelector('[data-status]').textContent='已读取与此段转写对应的录音。';
  }catch(error){if(valid())dialog.querySelector('[data-status]').textContent=error.message;}
}
document.addEventListener('change',event=>{
  const box=event.target.closest('[data-transcript-select]');if(!box)return;
  const id=Number(box.dataset.transcriptSelect),selected=new Set(state.customerTranscriptSelection||[]);
  if(box.checked){if(selected.size>=20){box.checked=false;toast('每次最多选择20段');return;}selected.add(id);}else selected.delete(id);
  state.customerTranscriptSelection=[...selected];document.querySelector('[data-transcript-case]').textContent=`整理所选段落（${selected.size}）`;
});
document.addEventListener('click',async event=>{
  const intelligenceButton=event.target.closest('[data-transcript-intelligence]');if(intelligenceButton){openCallIntelligence(Number(intelligenceButton.dataset.transcriptIntelligence));return;}
  const refresh=event.target.closest('[data-transcript-refresh]'),pager=event.target.closest('[data-transcript-page]'),play=event.target.closest('[data-transcript-audio]'),create=event.target.closest('[data-transcript-case]');
  if(refresh){loadCustomerTranscript(state.customerDetailId);return;}
  if(pager){loadCustomerTranscript(state.customerDetailId,Number(pager.dataset.transcriptPage));return;}
  if(play){const s=state.customerTranscript?.segments.content.find(s=>String(s.id)===play.dataset.transcriptAudio);if(s)playTranscriptSegment(s);return;}
  if(!create)return;
  const ids=state.customerTranscriptSelection||[];if(!ids.length){toast('请先选择需要整理的转写段落');return;}
  const account=state.auth.user?.id,customerId=state.customerDetailId;create.disabled=true;
  try {
    const source=await apiRequest(`/customers/${encodeURIComponent(customerId)}/transcript/case`,{method:'POST',body:JSON.stringify({segmentIds:ids})});
    const article=await apiRequest(`/knowledge/${source.id}`);
    if(state.auth.user?.id===account && state.customerDetailId===customerId)openCompanyCaseEditor(article,()=>{});
  }catch(error){toast(error.message);}finally{create.disabled=false;}
});

async function uploadRecordingFile(file, {callId, customerId, note = '', valid = () => true}) {
  const ticket = await apiRequest('/recording-uploads', {method: 'POST', body: JSON.stringify({callId, customerId, note, size: file.size,autoProcess:true,processingConsent:true})});
  if (!valid()) throw new Error('账号或页面已切换，请重新上传。');
  if (ticket.direct) {
    const response = await fetch(ticket.url, {method: 'PUT', headers: {'Content-Type': 'application/octet-stream'}, body: file,
      credentials: 'omit', referrerPolicy: 'no-referrer', signal: AbortSignal.timeout(15 * 60 * 1000)});
    if (!response.ok) throw new Error('录音文件上传失败，请重试。');
    if (!valid()) throw new Error('账号或页面已切换，请重新上传。');
    return apiRequest(`/recording-uploads/${encodeURIComponent(ticket.id)}/complete`, {method: 'POST', timeoutMs: 300000});
  }
  const body = new FormData(); body.append('file', file);
  body.append('autoProcess','true');body.append('processingConsent','true');
  if (callId == null) { body.append('customerId', customerId); body.append('note', note); }
  return apiRequest(callId == null ? '/call-uploads' : `/calls/${encodeURIComponent(callId)}/recording`,
    {method: 'POST', body, timeoutMs: 300000});
}

function openCallRecording(callId,{autoReview=false}={}) {
  closeCallRecording?.();
  const call = [...calls, ...(state.customerCalls || [])].find(item => String(item.id) === String(callId));
  if (!call || call.canAccessRecording === false) return;
  const accountId = state.auth.user?.id;
  const dialog = document.createElement('dialog');
  dialog.className = 'recording-dialog';
  dialog.setAttribute('aria-labelledby', 'recordingTitle');
  dialog.innerHTML = `<header><h2 id="recordingTitle">通话录音</h2><button type="button" class="button secondary" data-close>关闭</button></header>
    <p data-customer></p><p>上传已获得授权的销售与客户通话录音。支持 WAV、MP3、M4A、FLAC、OGG，最大 200MB。</p>
    <audio controls preload="none" hidden></audio><p role="status" data-status></p>
    <details data-history><summary>历史录音与转写</summary><div data-history-content></div></details>
    <form><label>选择录音<input type="file" accept=".wav,.mp3,.m4a,.flac,.ogg" required></label>
    <label><input type="checkbox" required>我有权上传此客户录音，并同意上传后由千问转写和建立私有知识检索，并由 DeepSeek 分析和生成跟进草稿；已有录音将保留历史版本。</label>
    <button class="button primary" type="submit">上传录音</button></form>
    <section><h3>录音转写</h3><label>转写方式<select data-transcription-provider><option value="local">本地转写</option><option value="cloud" selected>云端转写（千问）</option></select></label><p data-ai-status role="status"></p>
    <label><input data-ai-consent type="checkbox">我同意本次转写及案例入库；选择云端时录音发送至千问，已配置 AI 分析时转写文字会发送至分析服务。</label>
    <button type="button" class="button primary" data-analyze disabled>开始转写</button>
    <button type="button" class="button secondary" data-company-case hidden>整理为公司案例</button>
    <button type="button" class="button primary" data-open-intelligence hidden>AI分析与跟进模板</button><p data-intelligence-state role="status"></p>
    <p>转写和来源案例仅当前客户归属人和管理员可见。可将选定内容整理为公司案例后发布，其他员工只能查看已发布案例，不能查看原始转写或回听录音。配置 AI 分析服务后可自动整理来源案例。支持最长两小时、最大200MB；转写时间标记不代表精确逐句时间戳。</p>
    <details><summary>转写原文</summary><pre data-transcript></pre></details>
    <h4>AI 原始整理</h4><pre data-summary></pre>
    <section data-review hidden><h4>人工核对</h4><label>确认后的总结<textarea data-review-text rows="9" maxlength="50000"></textarea></label>
    <button type="button" class="button primary" data-save-review>确认保存</button><p data-review-status role="status"></p></section></section>`;
  dialog.querySelector('[data-customer]').textContent = `${call.customer || ''} · 通话销售：${call.agent || '未记录'} · ${call.recordingSource === 'UPLOAD' ? '上传录音' : '系统录音'} · ${call.started || ''}`;
  const status = dialog.querySelector('[data-status]');
  const audio = dialog.querySelector('audio');
  let objectUrl = null;
  let closed = false;
  let analysisTimer = null;
  let analysisRequest = 0;
  let intelligenceTimer=null;
  let reviewSnapshot = null;
  let mutationPending = false;
  let audioRequest = 0;
  const valid = () => !closed && state.auth.user?.id === accountId && Boolean(state.auth.token);
  const cleanup = () => {
    if (closed) return;
    closed = true;
    clearInterval(accountWatcher);
    clearTimeout(analysisTimer);
    clearTimeout(intelligenceTimer);
    audio.pause(); audio.removeAttribute('src'); audio.load();
    if (objectUrl) URL.revokeObjectURL(objectUrl);
    dialog.remove();
    closeCallRecording = null;
  };
  closeCallRecording = cleanup;
  const accountWatcher = setInterval(() => { if (!valid()) cleanup(); }, 500);
  dialog.addEventListener('close', cleanup);
  dialog.addEventListener('cancel', cleanup);
  dialog.querySelector('[data-close]').onclick = cleanup;
  const analyzeButton = dialog.querySelector('[data-analyze]');
  const analysisStatus = dialog.querySelector('[data-ai-status]');
  const providerSelect = dialog.querySelector('[data-transcription-provider]');
  const transcriptionUrl = () => `/calls/${encodeURIComponent(callId)}/recording/transcriptions/${providerSelect.value}`;
  providerSelect.onchange = () => {
    ++analysisRequest; clearTimeout(analysisTimer);
    dialog.querySelector('[data-transcript]').textContent = '';
    dialog.querySelector('[data-ai-consent]').checked = false;
    analyzeButton.disabled = true; loadAnalysis();
  };
  function showAnalysis(result) {
    if (!valid()) return;
    const labels = {NO_RECORDING: '请先上传录音', PENDING: '待转写', QUEUED: '已加入转写队列，等待处理…', PROCESSING: '正在转写，请稍候…', COMPLETED: '转写完成，请核对', FAILED: '转写失败，可重试'};
    analysisStatus.textContent = result.configured ? (result.error || labels[result.status] || result.status) : '管理员尚未配置AI服务';
    analyzeButton.disabled = !result.configured || ['NO_RECORDING', 'QUEUED', 'PROCESSING'].includes(result.status);
    analyzeButton.textContent=result.status==='COMPLETED'?'重新转写':'开始转写';
    dialog.querySelector('[data-transcript]').textContent = result.transcript || '';
    dialog.querySelector('[data-company-case]').hidden = result.status !== 'COMPLETED';
    const aiButton=dialog.querySelector('[data-open-intelligence]');aiButton.hidden=!result.segmentId;
    if(result.segmentId){aiButton.onclick=()=>{cleanup();openCallIntelligence(result.segmentId);};watchIntelligence(result.segmentId);}
    if(result.status==='COMPLETED' && state.customerDetailTab==='transcript' && String(state.customerDetailId)===String(call.customerId)) loadCustomerTranscript(state.customerDetailId);
    dialog.querySelector('[data-summary]').textContent = result.summary || '';
    dialog.querySelector('[data-summary]').hidden = !result.summary;
    dialog.querySelector('[data-summary]').previousElementSibling.hidden = !result.summary;
    if (result.status === 'COMPLETED' && result.summary) loadReview();
    else { reviewSnapshot = null; dialog.querySelector('[data-review]').hidden = true; }
    clearTimeout(analysisTimer);
    if (['QUEUED', 'PROCESSING'].includes(result.status)) analysisTimer = setTimeout(loadAnalysis, 3000);
  }
  async function watchIntelligence(id){
    clearTimeout(intelligenceTimer);
    try{
      const data=await apiRequest(`/transcript-segments/${id}/intelligence`);if(!valid())return;
      const labels={WAITING_CONFIG:'DeepSeek 待管理员配置；转写已保存',QUEUED:'AI分析排队中',PROCESSING:'正在分析并生成跟进模板',COMPLETED:'AI分析完成，可选择并修改跟进模板',WAITING_CONSENT:'可授权生成客户分析与跟进模板',NOT_REQUESTED:'可生成客户分析与跟进模板',FAILED:'AI分析失败，可重试'};
      dialog.querySelector('[data-intelligence-state]').textContent=data.confirmedAt?'销售已确认保存跟进':(!data.configured&&!data.analysis?'DeepSeek 待管理员配置；转写已保存':labels[data.state]||data.state);
      if(data.state==='COMPLETED'&&!data.confirmedAt&&autoReview){autoReview=false;cleanup();openCallWorkForCall(callId);return;}
      if(['QUEUED','PROCESSING'].includes(data.state))intelligenceTimer=setTimeout(()=>watchIntelligence(id),2500);
    }catch(error){if(valid())dialog.querySelector('[data-intelligence-state]').textContent=error.message;}
  }
  async function loadAnalysis() {
    const request = ++analysisRequest;
    try {
      const result = await apiRequest(transcriptionUrl());
      if (request === analysisRequest) showAnalysis(result);
    } catch (error) { if (valid() && request === analysisRequest) analysisStatus.textContent = error.message; }
  }
  async function loadReview() {
    const request = analysisRequest;
    try {
      const review = await apiRequest(`/calls/${encodeURIComponent(callId)}/recording/review`);
      if (!valid() || request !== analysisRequest) return;
      reviewSnapshot = review;
      dialog.querySelector('[data-review]').hidden = false;
      dialog.querySelector('[data-review-text]').value = review.text || '';
      dialog.querySelector('[data-review-status]').textContent = review.reviewedAt ? `已由 ${review.reviewer} 确认保存` : '尚未人工确认';
    } catch (error) { if (valid()) analysisStatus.textContent = error.message; }
  }
  dialog.querySelector('[data-save-review]').onclick = async event => {
    if (!reviewSnapshot || !valid() || mutationPending) return;
    const text = dialog.querySelector('[data-review-text]').value.trim();
    const message = dialog.querySelector('[data-review-status]');
    if (!text) { message.textContent = '请填写核对后的总结。'; return; }
    const button = event.currentTarget;
    const snapshot = reviewSnapshot;
    mutationPending = true;
    button.disabled = true;
    try {
      await apiRequest(`/calls/${encodeURIComponent(callId)}/recording/review`, {method: 'PUT', body: JSON.stringify({...snapshot, text})});
      if (!valid()) return;
      message.textContent = '核对结果已保存';
      snapshot.version += 1;
    } catch (error) { if (valid()) message.textContent = error.message; }
    finally { button.disabled = false; mutationPending = false; }
  };
  dialog.querySelector('[data-company-case]').onclick = async event => {
    if (!valid() || mutationPending) return;
    const button = event.currentTarget; button.disabled = true; mutationPending = true;
    try {
      const source = await apiRequest(`/calls/${encodeURIComponent(callId)}/recording/transcriptions/${providerSelect.value}/case`, {method: 'POST'});
      const article = await apiRequest(`/knowledge/${source.id}`);
      if (!valid()) return;
      cleanup(); openCompanyCaseEditor(article, () => {});
    } catch (error) { if (valid()) analysisStatus.textContent = error.message; }
    finally { button.disabled = false; mutationPending = false; }
  };
  analyzeButton.onclick = async () => {
    if (mutationPending || !valid()) return;
    if (!dialog.querySelector('[data-ai-consent]').checked) { analysisStatus.textContent = '请先确认本次录音的AI处理授权。'; return; }
    analyzeButton.disabled = true;
    mutationPending = true;
    providerSelect.disabled = true;
    const request = ++analysisRequest;
    clearTimeout(analysisTimer);
    try {
      autoReview=true;
      const result = await apiRequest(transcriptionUrl(), {method: 'POST', body: JSON.stringify({cloudConsent: providerSelect.value === 'cloud' && dialog.querySelector('[data-ai-consent]').checked,analysisConsent:true})});
      if (request === analysisRequest) showAnalysis(result);
    }
    catch (error) { if (valid()) { analysisStatus.textContent = error.message; analyzeButton.disabled = false; } }
    finally { mutationPending = false; providerSelect.disabled = false; }
  };
  async function loadAudio(revision) {
    const request = ++audioRequest;
    status.textContent = '正在读取录音…';
    try {
      const versionQuery = revision ? `?revision=${encodeURIComponent(revision)}` : '';
      const playback = await apiRequest(`/calls/${encodeURIComponent(callId)}/recording/playback${versionQuery}`);
      if (!valid() || request !== audioRequest) return;
      if (playback.url) {
        if (objectUrl) { URL.revokeObjectURL(objectUrl); objectUrl = null; }
        audio.src = playback.url; audio.hidden = false;
        status.textContent = '录音可播放，若链接过期请重新打开。';
        return;
      }
      const blob = await apiRequest(`/calls/${encodeURIComponent(callId)}/recording${versionQuery}`, { responseType: 'blob', timeoutMs: 60000 });
      if (!valid() || request !== audioRequest) return;
      if (objectUrl) URL.revokeObjectURL(objectUrl);
      objectUrl = URL.createObjectURL(blob);
      audio.src = objectUrl; audio.hidden = false;
      status.textContent = '录音已加载，可播放核对。';
    } catch (error) { if (valid() && request === audioRequest) status.textContent = error.message; }
  }
  async function loadHistory(page = 0) {
    const container = dialog.querySelector('[data-history-content]');
    container.textContent = '正在读取历史版本…';
    try {
      const result = await apiRequest(`/calls/${encodeURIComponent(callId)}/recording/history?page=${page}`);
      if (!valid()) return;
      container.replaceChildren();
      const current = document.createElement('button'); current.type = 'button'; current.className = 'button secondary';
      current.textContent = '播放当前录音'; current.onclick = () => loadAudio(); container.append(current);
      if (!result.content.length) container.append(document.createTextNode(' 暂无历史版本。'));
      for (const version of result.content) {
        const section = document.createElement('section');
        const button = document.createElement('button'); button.type = 'button'; button.className = 'button secondary';
        button.textContent = `播放历史录音 · ${formatDateTime(version.uploadedAt)}`;
        button.onclick = () => loadAudio(version.revision); section.append(button);
        const snapshot = JSON.parse(version.snapshot);
        const text = document.createElement('pre'); text.style.whiteSpace = 'pre-wrap';
        text.textContent = [snapshot.local?.transcript && `本地转写\n${snapshot.local.transcript}`,
          snapshot.cloud?.transcript && `云端转写\n${snapshot.cloud.transcript}`,
          snapshot.transcript && `原始转写\n${snapshot.transcript}`, snapshot.summary && `AI 总结\n${snapshot.summary}`,
          snapshot.reviewedSummary && `人工确认\n${snapshot.reviewedSummary}`].filter(Boolean).join('\n\n') || '此版本尚无已保存的转写内容。';
        section.append(text); container.append(section);
      }
      for (const [label, target] of [['上一页', page - 1], ['下一页', page + 1]]) {
        if (target < 0 || target >= result.totalPages) continue;
        const button = document.createElement('button'); button.type = 'button'; button.textContent = label;
        button.onclick = () => loadHistory(target); container.append(button);
      }
    } catch (error) { if (valid()) container.textContent = error.message; }
  }
  dialog.querySelector('[data-history]').ontoggle = event => { if (event.currentTarget.open) loadHistory(); };
  dialog.querySelector('form').onsubmit = async event => {
    event.preventDefault();
    const form = event.currentTarget;
    const file = form.querySelector('[type=file]').files[0];
    if (!file || !valid() || mutationPending) return;
    if (!file.size || file.size > 200 * 1024 * 1024) { status.textContent = '请选择非空且不超过200MB的录音。'; return; }
    const body = new FormData(); body.append('file', file);
    ++analysisRequest;
    ++audioRequest;
    mutationPending = true;
    clearTimeout(analysisTimer);
    const button = form.querySelector('button'); button.disabled = true;
    status.textContent = '正在上传…';
    try {
      await uploadRecordingFile(file, {callId, valid});
      autoReview=true;
      if (!valid()) return;
      form.reset();
      dialog.querySelector('[data-ai-consent]').checked = false;
      await loadAnalysis();
      await loadAudio();
    } catch (error) { if (valid()) status.textContent = error.message; }
    finally { button.disabled = false; mutationPending = false; if (valid()) loadAnalysis(); }
  };
  document.body.append(dialog);
  dialog.showModal();
  loadAudio();
  loadAnalysis();
}

document.addEventListener('click', event => {
  const button = event.target.closest('[data-call-recording]');
  if (button) openCallRecording(button.dataset.callRecording);
});

let customerCallsRequest = 0;
async function loadCustomerCalls(customerId, page = 0) {
  const request = ++customerCallsRequest, accountId = state.auth.user?.id;
  state.customerCalls = []; state.customerCallsLoading = true; state.customerCallsError = ''; render();
  const valid = () => request === customerCallsRequest && state.customerDetailId === customerId && accountId === state.auth.user?.id;
  try {
    const [result,sources,pending]=await Promise.all([apiRequest(`/customers/${encodeURIComponent(customerId)}/calls?page=${page}&size=20`),apiRequest(`/call-followup-work/sources?customerNo=${encodeURIComponent(customerId)}`).catch(()=>[]),apiRequest(`/call-followup-work?customerNo=${encodeURIComponent(customerId)}`).catch(()=>[])]);
    const data=pageContent(result),sourceByCall=new Map([...pending,...sources].map(s=>[s.callId,s]));
    if (valid()) { state.customerCalls = data.content.map(c=>({...normalizeCall(c),aiTitle:sourceByCall.get(c.id)?.title,followupStatus:sourceByCall.get(c.id)?.followupStatus})); state.customerCallsMeta = data; }
  } catch (error) { if (valid()) state.customerCallsError = error.message; }
  finally { if (valid()) { state.customerCallsLoading = false; render(); } }
}
function customerCallsView(customer) {
  const meta = state.customerCallsMeta;
  return `<section class="data-panel"><div class="data-toolbar"><div><h2>通话记录</h2><p>录音及 AI 分析随客户保留，通话销售不因客户交接而改变。</p></div><button class="button secondary" data-refresh-customer-calls>刷新</button></div>
  ${state.customerCallsLoading ? '<p role="status">正在加载通话记录…</p>' : state.customerCallsError ? `<p role="alert">${escapeHtml(state.customerCallsError)}</p>` : `<div class="table-wrap"><table class="data-table"><thead><tr><th>时间</th><th>通话销售</th><th>录音来源</th><th>类型</th><th>状态</th><th>时长</th><th>通话主题 / 备注</th><th>录音与 AI 分析</th></tr></thead><tbody>${(state.customerCalls || []).map(call => `<tr><td>${call.recordingSource === 'UPLOAD' && call.uploadedAt ? '上传：' + escapeHtml(formatDateTime(call.uploadedAt)) : '通话：' + escapeHtml(formatDateTime(call.startedAt))}</td><td>${escapeHtml(call.agent || '未记录')}</td><td>${call.recordingSource === 'UPLOAD' ? '上传录音' : '系统录音'}</td><td>${escapeHtml(call.direction)}</td><td>${escapeHtml(call.status)}</td><td>${formatDuration(call.durationSeconds)}</td><td>${escapeHtml(call.aiTitle||call.note||'—')}${call.followupStatus?`<br><small class="${call.followupStatus==='FOLLOWED'?'call-followed':'call-unfollowed'}">${call.followupStatus==='FOLLOWED'?'已跟进':'未跟进'}</small>`:''}</td><td><button class="button ghost" data-call-recording="${escapeHtml(String(call.id))}">播放 / AI 分析</button></td></tr>`).join('') || '<tr><td colspan="8" class="call-empty">暂无通话记录，点击右上角“上传录音”添加。</td></tr>'}</tbody></table></div>`}
  ${meta && !state.customerCallsLoading ? `<footer class="call-pagination"><span>共 ${meta.totalElements} 条</span><button data-customer-calls-page="${meta.page - 1}" ${meta.page <= 0 ? 'disabled' : ''}>上一页</button><span>${meta.page + 1} / ${Math.max(1, meta.totalPages)}</span><button data-customer-calls-page="${meta.page + 1}" ${meta.page + 1 >= meta.totalPages ? 'disabled' : ''}>下一页</button></footer>` : ''}</section>`;
}
async function openRecordingUpload(customerId) {
  closeCallRecording?.();
  const accountId = state.auth.user?.id;
  const customer = [...customers, ...state.poolCustomers].find(c => String(c.id) === String(customerId));
  const dialog = document.createElement('dialog'); dialog.className = 'recording-dialog';
  dialog.setAttribute('aria-labelledby', 'uploadRecordingTitle');
  dialog.innerHTML = `<header><h2 id="uploadRecordingTitle">上传录音</h2><button class="button secondary" data-close>关闭</button></header><form>
    <label>关联客户${customer ? `<p>${escapeHtml(customer.name)} · ${escapeHtml(String(customer.id))}</p>` : '<select name="customerId" required><option value="">正在加载客户…</option></select>'}</label>
    <label>录音文件<input name="file" type="file" accept=".wav,.mp3,.m4a,.flac,.ogg" required></label><p>支持 WAV、MP3、M4A、FLAC、OGG，最大 200MB。上传人自动记为通话销售，上传时间和录音时长由系统记录。</p>
    <label>备注<textarea name="note" maxlength="1000" rows="3"></textarea></label>
    <label><input type="checkbox" required>我有权上传，并同意上传后由千问转写和建立私有知识检索，并由 DeepSeek 分析和生成跟进草稿。</label>
    <p role="status" data-status></p><button class="button primary" type="submit">上传并保存</button></form>`;
  let closed = false;
  const valid = () => !closed && accountId === state.auth.user?.id && Boolean(state.auth.token);
  const cleanup = () => { closed = true; clearInterval(watcher); dialog.remove(); closeCallRecording = null; };
  const watcher = setInterval(() => { if (!valid()) cleanup(); }, 500);
  closeCallRecording = cleanup; dialog.querySelector('[data-close]').onclick = cleanup;
  dialog.addEventListener('cancel', cleanup); dialog.addEventListener('close', cleanup);
  const form = dialog.querySelector('form'), status = dialog.querySelector('[data-status]'), submit = form.querySelector('[type=submit]');
  form.onsubmit = async event => {
    event.preventDefault(); if (!valid() || submit.disabled) return;
    const file = form.elements.file.files[0];
    if (!file || !file.size || file.size > 200 * 1024 * 1024) { status.textContent = '请选择非空且不超过200MB的录音。'; return; }
    const body = new FormData(form); body.set('customerId', customer ? customer.id : form.elements.customerId.value);
    submit.disabled = true; status.textContent = '正在上传并保存…';
    try {
      const saved = normalizeCall(await uploadRecordingFile(file, {customerId: body.get('customerId'), note: body.get('note') || '', valid}));
      if (!valid()) return;
      cleanup(); calls.unshift(saved);
      if (state.view === 'customers' && String(state.customerDetailId) === String(saved.customerId)) {
        if(state.customerDetailTab==='transcript') await loadCustomerTranscript(state.customerDetailId);
        else { state.customerDetailTab = 'calls'; await loadCustomerCalls(state.customerDetailId); }
      } else await refreshCallsFromApi();
      if (!calls.some(c => c.id === saved.id)) state.customerCalls = [saved];
      openCallRecording(saved.id,{autoReview:true});
    } catch (error) { if (valid()) { status.textContent = error.message; submit.disabled = false; } }
  };
  document.body.append(dialog); dialog.showModal();
  if (!customer) {
    submit.disabled = true;
    try {
      const options = new Map();
      for (const endpoint of ['/customers', '/customers/pool']) {
        let page = 0, pages = 1;
        while (page < pages) {
          const data = pageContent(await apiRequest(`${endpoint}?page=${page}&size=100`)); if (!valid()) return;
          data.content.forEach(c => options.set(c.id, c)); pages = data.totalPages; page++;
        }
      }
      form.elements.customerId.innerHTML = '<option value="">请选择客户</option>' + [...options.values()].map(c => `<option value="${escapeHtml(String(c.id))}">${escapeHtml(c.name)} · ${escapeHtml(String(c.id))}</option>`).join(''); submit.disabled = false;
    } catch (error) { if (valid()) status.textContent = error.message; }
  }
}
document.addEventListener('click', event => {
  const upload = event.target.closest('[data-upload-recording]'); if (upload) openRecordingUpload(upload.dataset.uploadRecording);
  const page = event.target.closest('[data-customer-calls-page]'); if (page && !page.disabled) loadCustomerCalls(state.customerDetailId, Number(page.dataset.customerCallsPage));
  if (event.target.closest('[data-refresh-customer-calls]')) loadCustomerCalls(state.customerDetailId);
});
