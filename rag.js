let ragMemory={account:null,drafts:{},answers:{}};
function ragSession(){if(ragMemory.account!==state.auth.user?.id||!state.auth.token){ragMemory={account:state.auth.user?.id,drafts:{},answers:{}};document.querySelectorAll('[data-rag-owner]').forEach(dialog=>dialog.remove());}return ragMemory;}
setInterval(()=>ragSession(),1000);
function ragAskMarkup(key='company'){
  const memory=ragSession(),draft=memory.drafts[key]||'';
  return `<section class="rag-panel" data-rag-panel data-rag-key="${escapeHtml(key)}"><h3>知识库问答</h3><p>${key==='company'?'检索公司共享案例和通用知识，回答会附上引用依据。客户私有通话请在客户详情的AI通话分析中查看。':'结合本客户通话与公司知识给建议，引用原文可回查。'}</p><form data-rag-form><textarea name="question" maxlength="1200" rows="2" required placeholder="例如：客户拒绝邀约时，应该怎样继续沟通？">${escapeHtml(draft)}</textarea><button class="button primary" type="submit">询问知识库</button></form><p data-rag-status role="status"></p><div data-rag-answer></div></section>`;
}
function renderRagAnswer(container,result){
  container.replaceChildren();const answer=document.createElement('p');answer.className='rag-answer-text';answer.textContent=result.answer;container.append(answer);
  if(!result.citations?.length)return;
  const label=document.createElement('strong');label.textContent='引用依据';container.append(label);
  result.citations.forEach((citation,index)=>{
    const article=document.createElement('article');article.className='rag-citation';const title=document.createElement('strong');title.textContent=`${index+1}. ${citation.title}`;const quote=document.createElement('blockquote');quote.textContent=citation.quote;
    const button=document.createElement('button');button.type='button';button.className='button ghost';button.textContent=citation.articleId?'查看知识案例':'查看来源录音与原文';
    button.onclick=async()=>{const account=state.auth.user?.id;try{if(citation.articleId){const source=await apiRequest(`/knowledge/${citation.articleId}`);if(account===state.auth.user?.id)openKnowledgeArticle(source,()=>{});}else{
      const params=new URLSearchParams({id:citation.sourceId,customerNo:citation.customerNo,version:citation.version}),source=await apiRequest(`/knowledge/rag-source?${params}`);if(account!==state.auth.user?.id)return;
      const dialog=document.createElement('dialog');dialog.className='knowledge-dialog rag-source-dialog';dialog.dataset.ragOwner=account;dialog.innerHTML='<header><h2></h2><button type="button" class="button secondary" data-close>关闭</button></header><pre></pre><button type="button" class="button secondary" data-audio>回听对应录音</button>';
      dialog.querySelector('h2').textContent=source.title;dialog.querySelector('pre').textContent=source.content;dialog.querySelector('[data-close]').onclick=()=>dialog.remove();dialog.oncancel=()=>dialog.remove();dialog.querySelector('[data-audio]').onclick=async()=>{try{const segment=await apiRequest(`/customers/${encodeURIComponent(citation.customerNo)}/transcript/segments/${citation.segmentId}`);if(account===state.auth.user?.id)playTranscriptSegment(segment);}catch(e){toast(e.message);}};document.body.append(dialog);dialog.showModal();
    }}catch(e){toast(e.message);}};
    article.append(title,quote,button);container.append(article);
  });
}
function bindRagPanel(panel,{customerNo=null,segmentId=null,coaching=false}={}){
  if(!panel||panel.dataset.ragBound)return;panel.dataset.ragBound='true';
  const account=state.auth.user?.id,memory=ragSession(),key=panel.dataset.ragKey||'company';
  const valid=()=>panel.isConnected&&account===state.auth.user?.id&&Boolean(state.auth.token);
  const form=panel.querySelector('[data-rag-form]'),status=panel.querySelector('[data-rag-status]'),output=panel.querySelector('[data-rag-answer]');
  if(memory.answers[key])renderRagAnswer(output,memory.answers[key]);
  form.elements.question.oninput=()=>{if(valid())memory.drafts[key]=form.elements.question.value;};
  form.onsubmit=async event=>{
    event.preventDefault();const button=form.querySelector('button[type=submit]');if(button.disabled)return;button.disabled=true;status.textContent='正在检索相关资料并生成有来源的建议…';
    try{const result=await apiRequest('/knowledge/ask',{method:'POST',body:JSON.stringify({question:form.elements.question.value.trim(),customerNo,segmentId,coaching}),timeoutMs:330000});if(!valid())return;
      memory.answers[key]=result;renderRagAnswer(output,result);status.textContent=result.grounded?'已列出引用依据，请结合实际情况判断。':'没有足够依据，未生成无来源的结论。';
    }catch(e){if(valid())status.textContent=e.message;}finally{if(valid())button.disabled=false;}
  };
}
async function openRagAdvice(customerNo,segmentId){
  const account=state.auth.user?.id;
  try{await apiRequest(`/customers/${encodeURIComponent(customerNo)}/transcript/segments/${segmentId}`);}catch(e){toast(e.message);return;}
  if(account!==state.auth.user?.id)return;
  const key=`call:${customerNo}:${segmentId}`,memory=ragSession();if(!memory.drafts[key])memory.drafts[key]='请结合本次通话和已发布公司案例，分析客户诉求，指出销售沟通可改进之处，并给出下次行动建议。没有公司案例依据时请明确说明。';
  const dialog=document.createElement('dialog');dialog.className='knowledge-dialog rag-advice-dialog';dialog.dataset.ragOwner=state.auth.user?.id;dialog.innerHTML=`<header><h2>参考知识库给建议</h2><button type="button" class="button secondary" data-close>关闭</button></header>${ragAskMarkup(key)}`;
  document.body.append(dialog);dialog.showModal();dialog.querySelector('[data-close]').onclick=()=>dialog.remove();dialog.oncancel=()=>dialog.remove();bindRagPanel(dialog.querySelector('[data-rag-panel]'),{customerNo,segmentId,coaching:true});
}
function analysisFindings(title,items){return `<section><h4>${escapeHtml(title)}</h4>${items?.length?items.map(item=>`<p>${escapeHtml(item.text)}${item.evidence?`<small>原文依据：${escapeHtml(item.evidence)}</small>`:''}</p>`).join(''):'<p class="rag-empty">暂无明确依据，待补充。</p>'}</section>`;}
function callAdviceMarkup(view,includeAction=true){
  const action=includeAction&&view?.segmentId?`<button type="button" class="button secondary" data-rag-advice="${view.segmentId}" data-rag-customer="${escapeHtml(view.customerNo)}">参考知识库给建议</button>`:'';
  const a=view?.intelligence?.analysis;if(!a)return `<p class="rag-empty">通话分析完成后会自动显示客户诉求和销售改进建议。</p>${action}`;
  return `${analysisFindings('客户分析',a.customerAnalysis)}${analysisFindings('销售改进建议',a.salesCoaching)}${analysisFindings('下次行动与约定',a.reminders)}${action}`;
}
function customerAnalysisView(customer){
  return `<section class="data-panel customer-ai-analysis" data-customer-ai-analysis="${escapeHtml(customer.id)}"><div class="data-toolbar"><div><h2>AI客户复盘</h2><p>综合历次沟通，分析客户、改进沟通、准备下一次行动。仅当前归属人和管理员可查看。</p></div></div><section data-customer-review><p>正在读取累计复盘…</p></section><hr><h3>单次通话复盘</h3><label>本次通话<select data-ai-call-select><option>正在读取…</option></select></label><p data-ai-call-status role="status"></p><div data-ai-call-results></div></section>`;
}
function bindCustomerAnalysis(){
  const panel=document.querySelector('[data-customer-ai-analysis]');if(!panel||panel.dataset.bound)return;panel.dataset.bound='true';const account=state.auth.user?.id,customerNo=panel.dataset.customerAiAnalysis;
  const valid=()=>panel.isConnected&&account===state.auth.user?.id&&Boolean(state.auth.token);
  const status=panel.querySelector('[data-ai-call-status]'),select=panel.querySelector('[data-ai-call-select]'),result=panel.querySelector('[data-ai-call-results]');
  bindCustomerReview(panel.querySelector('[data-customer-review]'),customerNo);
  let rows=[];
  const show=()=>{const row=rows.find(r=>String(r.callId)===select.value);if(!row){result.innerHTML='';status.textContent='暂无可分析的通话，请先上传录音。';return;}
    const a=row.intelligence?.analysis;status.textContent=row.followUpId?'已提交跟进':'尚未提交跟进';result.innerHTML=`<h3>${escapeHtml(row.title||'通话分析')}</h3>${a?`<p>${escapeHtml(a.summary)}</p>`:'<p>正在等待自动分析完成。</p>'}${row.segmentId?`<div class="analysis-actions"><button type="button" class="button secondary" data-rag-advice="${row.segmentId}" data-rag-customer="${escapeHtml(row.customerNo)}">参考知识库给建议</button></div>`:''}<div class="analysis-cards">${callAdviceMarkup(row,false)}</div>`;
  };
  Promise.all([apiRequest(`/call-followup-work/sources?customerNo=${encodeURIComponent(customerNo)}`),apiRequest(`/call-followup-work?customerNo=${encodeURIComponent(customerNo)}`)]).then(([sources,pending])=>{
    if(!valid())return;rows=[...sources,...pending.filter(w=>!sources.some(s=>s.callId===w.callId))].sort((a,b)=>String(b.sourceTime).localeCompare(String(a.sourceTime)));
    select.innerHTML=rows.map(w=>`<option value="${w.callId}">${escapeHtml(w.sourceTime?.replace('T',' ').slice(0,19)||'')} · ${escapeHtml(w.title||'录音待总结')}</option>`).join('');select.onchange=show;show();
  }).catch(e=>{if(valid())status.textContent=e.message;});
}
document.addEventListener('click',event=>{const button=event.target.closest('[data-rag-advice]');if(button)openRagAdvice(button.dataset.ragCustomer,Number(button.dataset.ragAdvice));});
