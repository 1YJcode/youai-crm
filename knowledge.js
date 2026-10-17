let closeKnowledgeDialog = null;

function knowledgeBaseView() {
  return `<section class="page">${analyticsNav('知识库')}<div class="page-content knowledge-page" id="knowledgeRoot">
    <div class="knowledge-heading"><div><h2>知识库</h2><p>集中查阅服务流程、产品说明、沟通话术和常见问题。</p></div>${isAdmin() ? '<button class="button primary" data-knowledge-new>新增知识</button>' : ''}</div>
    <form class="knowledge-search"><input name="keyword" type="search" placeholder="搜索标题或正文" aria-label="搜索知识" maxlength="200"><select name="category" aria-label="知识分类"><option value="">全部分类</option></select><button class="button primary">搜索</button><button class="button secondary" type="reset">重置</button></form>
    <p data-knowledge-status role="status"></p><div class="knowledge-grid" data-knowledge-list></div><footer class="knowledge-pagination"><button class="button secondary" data-knowledge-prev>上一页</button><span data-knowledge-page></span><button class="button secondary" data-knowledge-next>下一页</button></footer>
  </div></section>`;
}

function bindKnowledgeBase() {
  const root = document.querySelector('#knowledgeRoot'); if (!root) return;
  const account = state.auth.user?.id;
  const valid = () => root.isConnected && state.auth.user?.id === account && Boolean(state.auth.token);
  const form = root.querySelector('form');
  const status = root.querySelector('[data-knowledge-status]');
  let page = 0, pages = 0, sequence = 0;
  async function load() {
    const request = ++sequence;
    status.textContent = '正在加载知识内容…';
    const params = new URLSearchParams({keyword:form.elements.keyword.value.trim(),category:form.elements.category.value,page:String(page)});
    try {
      const data = await apiRequest(`/knowledge?${params}`);
      if (!valid() || request !== sequence) return;
      pages = data.totalPages || 0;
      root.querySelector('[data-knowledge-list]').innerHTML = data.content.map(item => `<button type="button" class="knowledge-card" data-knowledge-id="${escapeHtml(String(item.id))}"><span class="pill blue">${escapeHtml(item.category)}</span><h3>${escapeHtml(item.title)}</h3><p>${escapeHtml(item.preview)}</p><small>更新于 ${escapeHtml(formatDateTime(item.updatedAt))}</small></button>`).join('');
      status.textContent = data.totalElements ? `共 ${data.totalElements} 篇知识` : (params.get('keyword') || params.get('category') ? '没有找到符合条件的知识，请调整搜索条件。' : '知识库还没有内容，管理员可点击“新增知识”开始整理。');
      root.querySelector('[data-knowledge-page]').textContent = pages ? `${page+1} / ${pages}` : '0 / 0';
      root.querySelector('[data-knowledge-prev]').disabled = page === 0;
      root.querySelector('[data-knowledge-next]').disabled = page+1 >= pages;
    } catch(error) {if(valid() && request===sequence) status.textContent=error.message;}
  }
  async function categories() {
    try {
      const items=await apiRequest('/knowledge/categories'); if(!valid()) return;
      const select=form.elements.category, selected=select.value;
      select.innerHTML='<option value="">全部分类</option>'+items.map(item=>`<option value="${escapeHtml(item)}">${escapeHtml(item)}</option>`).join('');
      select.value=items.includes(selected)?selected:'';
    } catch(error) {if(valid()) status.textContent=error.message;}
  }
  const reload=async()=>{await categories();await load();};
  form.onsubmit=event=>{event.preventDefault();page=0;load();};
  form.onreset=event=>{event.preventDefault();form.elements.keyword.value='';form.elements.category.value='';page=0;load();};
  root.querySelector('[data-knowledge-prev]').onclick=()=>{if(page>0){page--;load();}};
  root.querySelector('[data-knowledge-next]').onclick=()=>{if(page+1<pages){page++;load();}};
  root.querySelector('[data-knowledge-new]')?.addEventListener('click',()=>openKnowledgeArticle(null,reload));
  root.addEventListener('click',async event=>{
    const card=event.target.closest('[data-knowledge-id]'); if(!card) return;
    card.disabled=true;
    try {const article=await apiRequest(`/knowledge/${card.dataset.knowledgeId}`);if(valid()) openKnowledgeArticle(article,reload);}
    catch(error){if(valid()) status.textContent=error.message;}
    finally{card.disabled=false;}
  });
  reload();
}

function openKnowledgeArticle(article,onSave) {
  closeKnowledgeDialog?.();
  const account=state.auth.user?.id;
  const dialog=document.createElement('dialog'); dialog.className='knowledge-dialog';
  dialog.setAttribute('aria-labelledby','knowledgeDialogTitle');
  dialog.innerHTML=`<header><h2 id="knowledgeDialogTitle">${article?'知识详情':'新增知识'}</h2><button type="button" class="button secondary" data-close>关闭</button></header>
    <form><label>标题<input name="title" required maxlength="160"></label><label>分类<input name="category" required maxlength="64" placeholder="例如：服务流程、产品说明、沟通话术"></label><label>正文<textarea name="content" required maxlength="100000" rows="16"></textarea></label>
    <p data-meta></p><p data-status role="status"></p><footer>${isAdmin()?'<button class="button primary" type="submit">保存知识</button>':''}${isAdmin() && article?'<button class="button secondary" type="button" data-delete>删除</button>':''}</footer></form>`;
  const form=dialog.querySelector('form'), message=dialog.querySelector('[data-status]');
  form.elements.title.value=article?.title||'';form.elements.category.value=article?.category||'';form.elements.content.value=article?.content||'';
  if(article) dialog.querySelector('[data-meta]').textContent=`最后更新：${formatDateTime(article.updatedAt)} · ${article.updatedBy}`;
  if(!isAdmin()) [...form.querySelectorAll('input,textarea')].forEach(input=>input.readOnly=true);
  let closed=false, deleting=false;
  const valid=()=>!closed && state.auth.user?.id===account && Boolean(state.auth.token);
  const cleanup=()=>{if(closed)return;closed=true;clearInterval(watcher);dialog.remove();closeKnowledgeDialog=null;};
  const watcher=setInterval(()=>{if(!valid())cleanup();},500);
  closeKnowledgeDialog=cleanup;
  dialog.querySelector('[data-close]').onclick=cleanup;dialog.oncancel=cleanup;dialog.onclose=cleanup;
  form.onsubmit=async event=>{
    event.preventDefault();if(!isAdmin()||!valid())return;
    const data={title:form.elements.title.value.trim(),category:form.elements.category.value.trim(),content:form.elements.content.value.trim(),version:article?.version};
    if(!data.title||!data.category||!data.content){message.textContent='请填写标题、分类和正文。';return;}
    const buttons=[...form.querySelectorAll('button')];buttons.forEach(b=>b.disabled=true);
    try{await apiRequest(article?`/knowledge/${article.id}`:'/knowledge',{method:article?'PUT':'POST',body:JSON.stringify(data)});if(valid()){cleanup();onSave();toast('知识内容已保存');}}
    catch(error){if(valid())message.textContent=error.message;}
    finally{buttons.forEach(b=>b.disabled=false);}
  };
  dialog.querySelector('[data-delete]')?.addEventListener('click',async event=>{
    if(!deleting){deleting=true;event.currentTarget.textContent='再次点击确认删除';return;}
    const button=event.currentTarget;button.disabled=true;
    try{await apiRequest(`/knowledge/${article.id}?version=${article.version}`,{method:'DELETE'});if(valid()){cleanup();onSave();toast('知识内容已删除');}}
    catch(error){if(valid())message.textContent=error.message;button.disabled=false;}
  });
  document.body.append(dialog);dialog.showModal();
}
