// Per-call drafts stay in the write-follow-up form until its normal submit.
async function openCallWorkInbox() {
  const account=state.auth.user?.id;
  try {
    const rows=await apiRequest('/call-followup-work');if(state.auth.user?.id!==account)return;
    const dialog=document.createElement('dialog');dialog.className='knowledge-dialog call-work-inbox';
    dialog.innerHTML=`<header><h2>待写通话跟进 · ${rows.length}</h2><button type="button" class="button secondary" data-close>关闭</button></header><div>${rows.map(w=>`<article class="call-work-row"><strong>${escapeHtml(w.title||w.customerName)}</strong><small> · 未跟进</small><p>${escapeHtml(w.sourceTime?.replace('T',' ').slice(0,19)||'')} · 销售：${escapeHtml(w.salesName)} · 录音通话时长：${escapeHtml(formatTranscriptDuration(w.durationSeconds))}</p><button class="button primary" data-write-call="${w.callId}">写本次跟进</button></article>`).join('')||'<p>暂无待写通话跟进</p>'}</div>`;
    document.body.append(dialog);dialog.showModal();const close=()=>dialog.remove();dialog.querySelector('[data-close]').onclick=close;dialog.oncancel=close;
    dialog.onclick=e=>{const b=e.target.closest('[data-write-call]');if(b){const w=rows.find(w=>String(w.callId)===b.dataset.writeCall);close();openCallWorkForm(w);}};
  }catch(e){toast(e.message);}
}
function openCallWorkForm(w){
  openBusinessModal('task',{corner:true,customerId:w.customerNo,customer:w.customerName,sourceCallId:w.callId,dueAt:new Date().toISOString()});
  const type=document.querySelector('#businessForm select[name=type]');if(type)type.value='电话';
}
async function openCallWorkForCall(callId){
  const account=state.auth.user?.id;
  try{const rows=await apiRequest('/call-followup-work');if(state.auth.user?.id!==account)return;
    const w=rows.find(w=>String(w.callId)===String(callId));if(w)openCallWorkForm(w);else toast('本次通话已完成跟进，可在客户跟进记录中查看');
  }catch(e){toast(e.message);}
}
function mountCallFollowup(form,setTemplate,syncContent){
  if(form.dataset.corner!=='true')return;
  form.querySelector('.call-draft-panel')?.remove();
  const customerId=form.elements.customerId?.value,account=state.auth.user?.id,session=Symbol();form.callDraftSession=session;
  form.dataset.callWorkLoading='true';
  const panel=document.createElement('section');panel.className='form-span-2 call-draft-panel';
  panel.innerHTML='<label class="call-draft-source"><span>本次通话 <small data-unfollowed-count></small></span><select data-call-work-select><option value="">不关联通话，手动填写</option></select></label><p class="call-draft-meta" data-call-draft-meta hidden></p><p class="call-draft-status" data-call-draft-state role="status"></p><div class="inline-actions" data-call-actions></div><details class="call-coaching"><summary>AI通话建议</summary><div data-call-coaching></div></details>';
  form.querySelector('.followup-template-field').before(panel);
  const select=panel.querySelector('select'),status=panel.querySelector('[data-call-draft-state]'),actions=panel.querySelector('[data-call-actions]');
  let rows=[],chosen=null,timer=null,requestId=0,templateMode='ai';const edits=new Map();
  const valid=()=>panel.isConnected&&form.callDraftSession===session&&state.auth.user?.id===account&&Boolean(state.auth.token);
  const clearSource=()=>{delete form.dataset.aiSegmentId;delete form.dataset.aiVersion;delete form.dataset.aiTemplate;};
  form.clearAiDraftSource=clearSource;
  const templateOptions=form.querySelector('[data-followup-template-options]');
  const templateButtons=[...templateOptions.querySelectorAll('[data-followup-template]')];
  for(const button of templateButtons){
    let wrap=button.closest('.call-draft-choice');if(!wrap){wrap=document.createElement('div');wrap.className='call-draft-choice';button.before(wrap);wrap.append(button);}
    if(!wrap.querySelector('.call-draft-preview')){const preview=document.createElement('div');preview.className='call-draft-preview';preview.hidden=true;preview.setAttribute('role','tooltip');wrap.append(preview);}
    const positionPreview=()=>{
      const preview=wrap.querySelector('.call-draft-preview'),rect=button.getBoundingClientRect(),bounds=form.getBoundingClientRect();
      const width=Math.min(420,bounds.width-32,window.innerWidth-32),left=Math.max(bounds.left+16,Math.min(rect.left,bounds.right-width-16));
      preview.style.width=width+'px';preview.style.left=(left-wrap.getBoundingClientRect().left)+'px';
      const below=Math.min(bounds.bottom,window.innerHeight)-rect.bottom-16,above=rect.top-Math.max(bounds.top,0)-16,flip=below<150&&above>below;
      preview.style.top=flip?'auto':'100%';preview.style.bottom=flip?'100%':'auto';preview.style.maxHeight=Math.max(100,Math.min(330,flip?above:below))+'px';
    };
    button.onmouseenter=positionPreview;button.onfocus=positionPreview;
  }
  function updateTemplates(){
    const i=chosen?.intelligence,ready=templateMode==='ai'&&i?.state==='COMPLETED'&&Boolean(i.analysis);
    for(const button of templateButtons){
      const name=button.dataset.followupTemplate,draft=ready?i.analysis.templates.find(t=>t.name===name):null;
      button.textContent=name+(ready&&name===i.analysis.recommendedTemplate?'（推荐）':'');button.classList.toggle('is-ai-ready',Boolean(draft));
      const preview=button.closest('.call-draft-choice').querySelector('.call-draft-preview');preview.hidden=!draft;
      if(draft){button.dataset.aiDraft=name;preview.replaceChildren();const title=document.createElement('strong');title.textContent=name+' · AI跟进草稿';const body=document.createElement('div');body.className='call-draft-preview-content';body.innerHTML=followupContentEditorHtml(edits.get(`${chosen.callId}:${name}`)||draft.content,name);preview.append(title,body);}
      else{delete button.dataset.aiDraft;preview.replaceChildren();}
    }
  }
  async function adopt(){
    if(!chosen?.historical||chosen.followUpId)return;
    const selected=chosen;const result=await apiRequest(`/call-followup-work/from-transcript/${chosen.segmentId}`,{method:'POST'});
    if(!valid()||chosen!==selected)throw new Error('来源已切换，请重新选择草稿');Object.assign(chosen,result,{historical:false});form.dataset.callWorkId=chosen.callId;
  }
  form.prepareCallFollowup=adopt;
  async function applyDraft(draft){
    const selected=chosen,current=form.elements.title.value;
    if(current.trim()&&!form.dataset.aiTemplate&&!window.confirm('选用草稿将替换当前跟进内容，是否继续？'))return;
    try{await adopt();if(!valid()||chosen!==selected)return;}catch(e){if(valid())toast(e.message);return;}
    if(form.dataset.aiTemplate)edits.set(`${chosen.callId}:${form.dataset.aiTemplate}`,current);
    setTemplate(draft.name,edits.get(`${chosen.callId}:${draft.name}`)||draft.content);
    if(!chosen.followUpId){form.dataset.aiSegmentId=chosen.segmentId;form.dataset.aiVersion=chosen.intelligence.version;form.dataset.aiTemplate=draft.name;const type=form.elements.type;if(!type.value)type.value='电话';}
    else form.querySelectorAll('[contenteditable]').forEach(el=>el.setAttribute('contenteditable','false'));
    syncContent();updateTemplates();
  }
  form.useAiFollowupTemplate=name=>{
    if(templateMode==='original')return false;
    const i=chosen?.intelligence,draft=i?.state==='COMPLETED'?i.analysis?.templates.find(t=>t.name===name):null;
    if(!draft)return false;applyDraft(draft);return true;
  };
  form.querySelectorAll('[data-template-mode]').forEach(button=>button.onclick=()=>{
    templateMode=button.dataset.templateMode;
    form.querySelectorAll('[data-template-mode]').forEach(b=>b.setAttribute('aria-pressed',String(b===button)));
    form.querySelector('[data-template-mode-hint]').textContent=templateMode==='original'?'原始字段与提示，点击后手动填写':'按原模板字段自动填写';
    templateOptions.hidden=false;form.querySelector('[data-followup-template-toggle]').textContent='收起模板';form.querySelector('[data-followup-template-toggle]').setAttribute('aria-expanded','true');updateTemplates();
  });
  function action(label,handler,attribute){const b=document.createElement('button');b.type='button';b.className='button secondary';b.textContent=label;if(attribute)b.setAttribute(attribute,'');b.onclick=handler;actions.append(b);return b;}
  function render(){
    clearTimeout(timer);actions.replaceChildren();updateTemplates();panel.querySelector("[data-call-coaching]").innerHTML=callAdviceMarkup(chosen);
    const count=rows.filter(w=>!w.followUpId).length;panel.querySelector('[data-unfollowed-count]').textContent=count?`${count} 通未跟进`:'';
    const meta=panel.querySelector('[data-call-draft-meta]');meta.hidden=!chosen;
    const submit=form.querySelector('button[type=submit]');if(submit)submit.disabled=Boolean(chosen?.followUpId);
    if(!chosen){
      status.textContent=!customerId?'请在上方选择本次跟进的客户。':rows.length?'请选择本次跟进对应的通话。':'该客户暂无可用录音，请先上传录音；转写和全部模板草稿会自动生成。';
      if(customerId)action('上传客户录音',()=>{if(form.elements.title.value.trim()&&!window.confirm('上传录音将关闭当前窗口，请确认已保留手写内容。是否继续？'))return;closeBusinessModal();openRecordingUpload(customerId);},'data-call-ai-upload');return;
    }
    meta.textContent=`销售：${chosen.salesName} · 录音通话时长：${formatTranscriptDuration(chosen.durationSeconds)}`+(chosen.uploadedByName&&chosen.uploadedByName!==chosen.salesName?` · 上传：${chosen.uploadedByName}`:'');
    if(chosen.followUpId){status.textContent='已跟进 · 可预览模板，原跟进记录已保存';action('查看跟进记录',async()=>{closeBusinessModal();await openCustomer(customerId);state.customerDetailTab='follow';await loadCustomerFollowUps(customerId);});return;}
    const labels={WAITING_TRANSCRIPT:'未跟进 · 正在等待转写，完成后自动生成模板草稿',NOT_REQUESTED:'未跟进 · 正在自动排队生成模板草稿',WAITING_CONSENT:'未跟进 · 正在自动排队生成模板草稿',WAITING_CONFIG:'未跟进 · AI服务待配置，可先手动填写',QUEUED:'未跟进 · 全部模板草稿已排队',PROCESSING:'未跟进 · 正在生成标题和全部模板草稿',FAILED:'未跟进 · 草稿生成失败，可手动填写或重试',COMPLETED:'未跟进 · 悬浮下方模板预览，选中后可修改，提交才算完成'};
    status.textContent=labels[chosen.analysisState]||chosen.analysisState;
    if(chosen.analysisState==='FAILED')action('重试分析',async()=>{try{await apiRequest(`/transcript-segments/${chosen.segmentId}/intelligence`,{method:'POST',body:JSON.stringify({processingConsent:true})});if(valid())refresh();}catch(e){if(valid())toast(e.message);}});
    if(['WAITING_TRANSCRIPT','NOT_REQUESTED','WAITING_CONSENT','QUEUED','PROCESSING','WAITING_CONFIG'].includes(chosen.analysisState))timer=setTimeout(refresh,chosen.analysisState==='WAITING_CONFIG'?15000:3000);
  }
  async function refresh(){
    if(!valid()||!customerId)return;
    const n=++requestId;
    try{
      const [pending,sources]=await Promise.all([apiRequest(`/call-followup-work?customerNo=${encodeURIComponent(customerId)}`),apiRequest(`/call-followup-work/sources?customerNo=${encodeURIComponent(customerId)}`)]);
      if(!valid()||n!==requestId)return;
      rows=[...pending,...sources.filter(s=>!pending.some(w=>w.callId===s.callId)).map(s=>({...s,historical:!s.followUpId}))].sort((a,b)=>String(b.sourceTime||'').localeCompare(String(a.sourceTime||''))||b.callId-a.callId);
      const oldId=select.dataset.initialized?select.value:form.dataset.initialCallId;
      select.innerHTML='<option value="">不关联通话，手动填写</option>'+rows.map(w=>`<option value="${w.callId}">${w.followUpId?'[已跟进]':'[未跟进]'} ${escapeHtml(w.sourceTime?.replace('T',' ').slice(0,19)||'')} · ${escapeHtml(w.title||'录音待总结')}</option>`).join('');
      select.value=String(oldId&&rows.some(w=>String(w.callId)===String(oldId))?oldId:(!select.dataset.initialized?rows[0]?.callId||'':''));select.dataset.initialized='true';
      chosen=rows.find(w=>String(w.callId)===select.value)||null;
      if(chosen&&!chosen.historical)form.dataset.callWorkId=chosen.callId;else delete form.dataset.callWorkId;
      render();
    }catch(e){if(valid())status.textContent=e.message;}finally{if(valid())delete form.dataset.callWorkLoading;}
  }
  select.onchange=()=>{
    if(form.elements.title.value.trim()&&!window.confirm('切换通话会清空当前正文，是否继续？')){select.value=chosen?String(chosen.callId):'';return;}
    clearSource();setTemplate('');form.elements.title.value='';form.querySelector('[data-followup-rich-content]').replaceChildren();syncContent();
    chosen=rows.find(w=>String(w.callId)===select.value)||null;if(chosen&&!chosen.historical)form.dataset.callWorkId=chosen.callId;else delete form.dataset.callWorkId;render();
  };
  form.querySelectorAll('[data-template-mode]').forEach(b=>b.setAttribute('aria-pressed',String(b.dataset.templateMode===templateMode)));
  form.querySelector('[data-template-mode-hint]').textContent='按原模板字段自动填写';
  updateTemplates();
  if(customerId)refresh();else{delete form.dataset.callWorkLoading;render();}
}
function callFollowupSourceMarkup(source){
  if(!source)return '';
  return `<details class="followup-source" data-followup-source-details="${source.callId}" ${state.followupSourceOpen?.[source.callId]?'open':''}><summary>${source.generationMethod==='AI_CONFIRMED'?'AI草稿经确认':'人工填写'} · ${escapeHtml(source.template||'跟进记录')} · 来源：${escapeHtml(source.title||'通话录音')}</summary><p>通话销售：${escapeHtml(source.salesName||'未记录')} · 上传人：${escapeHtml(source.uploadedByName||'不适用')} · 提交人：${escapeHtml(source.submittedByName||'历史操作人')}</p><p>${source.timeKind==='CALL'?'通话时间':'录音上传时间'}：${escapeHtml(source.sourceTime?.replace('T',' ')||'未记录')} · 录音通话时长：${formatTranscriptDuration(source.durationSeconds)}</p><p>提交时间：${escapeHtml(source.submittedAt?.replace('T',' ')||'未记录')}</p>${source.segmentId?`<button type="button" class="button ghost" data-followup-source-segment="${source.segmentId}">查看来源原文与录音</button>`:''}</details>`;
}
document.addEventListener('click',async event=>{
  const summary=event.target.closest('[data-followup-source-details] > summary');
  if(summary){event.preventDefault();const details=summary.parentElement;details.open=!details.open;state.followupSourceOpen??={};state.followupSourceOpen[details.dataset.followupSourceDetails]=details.open;return;}
  const button=event.target.closest('[data-followup-source-segment]');if(!button)return;
  const id=Number(button.dataset.followupSourceSegment),customerId=state.customerDetailId,account=state.auth.user?.id;
  try{let page=0,segment=null;do{const archive=await apiRequest(`/customers/${encodeURIComponent(customerId)}/transcript?page=${page}`);segment=archive.segments.content.find(s=>s.id===id);if(segment||archive.segments.last)break;page++;}while(page<100);
    if(account!==state.auth.user?.id)return;if(!segment){toast('未找到来源转写');return;}playTranscriptSegment(segment);
  }catch(e){toast(e.message);}
});
