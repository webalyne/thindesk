const $ = selector => document.querySelector(selector);
const labels = {ABERTO:'Aberto',EM_ANDAMENTO:'Em andamento',CONCLUIDO:'Concluído'};
const colors = {ABERTO:'#c49a57',EM_ANDAMENTO:'#6a91b0',CONCLUIDO:'#4e8b81'};
const state = {page:1,pages:1,filter:null,options:null,detail:null,edit:null,legacy:false,controller:null,loading:false,loaded:false};
const esc = text => String(text??'').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'})[c]);
const stamp = value => value ? new Intl.DateTimeFormat('pt-BR',{timeZone:'America/Sao_Paulo',day:'2-digit',month:'2-digit',hour:'2-digit',minute:'2-digit'}).format(new Date(value)) : 'Sem data';
const short = value => new Intl.DateTimeFormat('pt-BR',{timeZone:'UTC',day:'2-digit',month:'2-digit'}).format(new Date(value+'T12:00:00Z'));
const today = () => {
  const parts=new Intl.DateTimeFormat('en-CA',{timeZone:'America/Sao_Paulo',year:'numeric',month:'2-digit',day:'2-digit'}).formatToParts(new Date());
  return ['year','month','day'].map(k=>parts.find(p=>p.type===k).value).join('-');
};
let toastTimer;
function notify(message) {
  clearTimeout(toastTimer);$('#toast').textContent=message;$('#toast').hidden=false;
  toastTimer=setTimeout(()=>{$('#toast').hidden=true;},4000);
}
async function api(path,options={}) {
  const headers = {...options.headers};
  if(options.body) {
    headers['Content-Type']='application/json';
    headers[$('meta[name="_csrf_header"]').content]=$('meta[name="_csrf"]').content;
  }
  let response;
  try { response=await fetch(path,{...options,headers,signal:options.signal||AbortSignal.timeout(10000),credentials:'same-origin'}); }
  catch(error) { if(error.name==='AbortError') throw error; throw new Error('Não foi possível conectar à API. Verifique o servidor e tente novamente.'); }
  if(response.status===401) { window.location.assign('/login');throw new Error('Sessão encerrada. Entre novamente.'); }
  const data=await response.json();
  if(!response.ok) { const error=new Error(data.mensagem||'Não foi possível concluir a solicitação.');error.status=response.status;throw error; }
  return data;
}
function period(days) {
  $('#fim').value=today();const d=new Date(today()+'T12:00:00Z');d.setUTCDate(d.getUTCDate()-days+1);$('#inicio').value=d.toISOString().slice(0,10);
  document.querySelectorAll('[data-dias]').forEach(b=>b.classList.toggle('selected',Number(b.dataset.dias)===days));
}
function filters() {
  const result=new URLSearchParams();
  for(const [key,value] of new FormData($('#filtros'))) if(value) result.set(key,value);
  if($('#busca').value.trim()) result.set('busca',$('#busca').value.trim());
  return result;
}
async function options() {
  state.options=await api('/api/dashboard/filtros');
  for(const [key,target,caption] of [['tipos','#tipo-filtro','Todos os tipos'],['tecnicos','#tecnico-filtro','Todos os técnicos'],['usuarios','#usuario-filtro','Todos os usuários']]) {
    const selected=$(target).value;
    $(target).innerHTML='<option value="">'+caption+'</option>'+state.options[key].map(v=>'<option value="'+esc(v)+'">'+esc(v)+'</option>').join('');
    $(target).value=selected;
    $('#'+key).innerHTML=state.options[key].map(v=>'<option value="'+esc(v)+'"></option>').join('');
  }
}
async function load(reset=false) {
  if(reset) state.page=1;
  state.controller?.abort();const controller=new AbortController();state.controller=controller;state.loading=true;
  $('#dados').setAttribute('aria-busy','true');$('#atualizar').disabled=true;
  try {
    if(!state.options) await options();
    const query=state.filter||filters();
    const listQuery=new URLSearchParams(query);listQuery.set('pagina',state.page);listQuery.set('limite','8');
    if(state.legacy) listQuery.set('semData','true');
    const signal=AbortSignal.any([controller.signal,AbortSignal.timeout(12000)]);
    const [dash,list]=await Promise.all([api('/api/dashboard?'+query,{signal}),api('/api/chamados?'+listQuery,{signal})]);
    if(controller.signal.aborted) return;
    if(state.page>Math.max(1,list.paginas)) {state.page=Math.max(1,list.paginas);return await load();}
    state.filter=new URLSearchParams(query);
    renderDashboard(dash);renderList(list);
    state.loaded=true;$('#erro').hidden=true;
    $('#atualizacao').textContent='Atualizado às '+new Intl.DateTimeFormat('pt-BR',{timeZone:'America/Sao_Paulo',hour:'2-digit',minute:'2-digit'}).format(new Date(dash.atualizadoEm));
  } catch(error) {
    if(!controller.signal.aborted) {
      $('#erro span').textContent=error.message+(state.loaded?' A tela mantém a última consulta.':'');
      $('#erro').hidden=false;$('#atualizacao').textContent='Atualização pendente';
    }
  } finally {
    if(state.controller===controller) {state.loading=false;$('#dados').setAttribute('aria-busy','false');$('#atualizar').disabled=false;}
  }
}
function renderDashboard(d) {
  const i=d.indicadores;
  $('#total').textContent=i.total;$('#abertos').textContent=i.abertos;$('#andamento').textContent=i.emAndamento;$('#atendidos').textContent=i.atendidos;
  $('#tempo').textContent=i.tempoMedioMinutos===null?'Sem dados':new Intl.NumberFormat('pt-BR',{maximumFractionDigits:1}).format(i.tempoMedioMinutos)+' min';
  const rows=d.serie;
  const w=650,h=210,left=34,top=15,bottom=33,pw=w-left-15,ph=h-top-bottom;
  const tick=Math.max(1,Math.ceil(Math.max(4,...rows.map(r=>r.quantidade))/4)),max=tick*4;
  const x=n=>left+(rows.length===1?pw/2:n*pw/(rows.length-1));
  const y=n=>top+ph-n/max*ph;
  let svg='';
  for(let n=0;n<=4;n++) svg+='<path class="grid" d="M'+left+' '+y(n*tick)+'H'+(w-15)+'" fill="none"/><text x="'+(left-8)+'" y="'+(y(n*tick)+3)+'" text-anchor="end">'+n*tick+'</text>';
  const path=rows.map((r,n)=>(n?'L':'M')+x(n)+','+y(r.quantidade)).join(' ');
  svg+='<path d="'+path+' L'+x(rows.length-1)+','+y(0)+' L'+x(0)+','+y(0)+'Z" fill="#eaf3f3"/><path d="'+path+'" fill="none" stroke="#3c7c83" stroke-width="2" vector-effect="non-scaling-stroke"/>';
  const stride=Math.max(1,Math.ceil(rows.length/8));
  rows.forEach((r,n)=>{
    if(n===rows.length-1 || (n%stride===0 && n<=rows.length-1-Math.max(1,Math.floor(stride/2)))) svg+='<text x="'+x(n)+'" y="'+(h-9)+'" text-anchor="middle">'+short(r.data)+'</text>';
    if(rows.length<=31) svg+='<circle cx="'+x(n)+'" cy="'+y(r.quantidade)+'" r="3" fill="#3c7c83" stroke="white" stroke-width="1.5"><title>'+short(r.data)+': '+r.quantidade+' atendidos</title></circle>';
  });
  $('#grafico').innerHTML='<svg viewBox="0 0 '+w+' '+h+'" role="img" aria-label="Chamados concluídos por período. Consulte também a tabela de valores.">'+svg+'</svg>';
  $('#serie-tabela').innerHTML=rows.map(r=>'<tr><td>'+short(r.data)+'</td><td>'+r.quantidade+'</td></tr>').join('');
  const total=d.situacao.reduce((sum,r)=>sum+r.quantidade,0),circ=2*Math.PI*54;let offset=0;
  const circles=d.situacao.map(r=>{
    const length=total?r.quantidade/total*circ:0;
    const circle=length?'<circle cx="75" cy="75" r="54" fill="none" stroke="'+colors[r.status]+'" stroke-width="12" stroke-dasharray="'+Math.max(0,length-2)+' '+circ+'" stroke-dashoffset="'+(-offset)+'" transform="rotate(-90 75 75)"/>':'';
    offset+=length;return circle;
  }).join('');
  $('#rosca').innerHTML='<svg viewBox="0 0 150 150" role="img" aria-label="Distribuição dos chamados por status"><circle cx="75" cy="75" r="54" fill="none" stroke="#edf2f4" stroke-width="12"/>'+circles+'<text x="75" y="76" text-anchor="middle" class="number">'+total+'</text><text x="75" y="94" text-anchor="middle" class="caption">chamados no período</text></svg>';
  $('#situacao').innerHTML=d.situacao.map(r=>'<div class="status-item"><i class="dot '+({ABERTO:'amber',EM_ANDAMENTO:'blue',CONCLUIDO:'green'})[r.status]+'"></i>'+esc(labels[r.status])+'<strong>'+r.quantidade+'</strong></div>').join('');
  $('#ranking').innerHTML=d.ranking.length?d.ranking.map((r,n)=>'<div class="rank-row"><span class="rank-index">0'+(n+1)+'</span><span class="rank-initial">'+esc(r.tipo.slice(0,1))+'</span><span class="rank-name">'+esc(r.tipo)+'</span><strong class="rank-value">'+r.quantidade+'</strong></div>').join(''):'<div class="empty">Sem atendimentos concluídos para os filtros selecionados.</div>';
  $('#legado').hidden=!i.semData&&!state.legacy;
  $('#legado span').textContent=state.legacy?'Exibindo chamados sem data de abertura.':i.semData+' chamados sem data de abertura não entram nos indicadores do período.';
  $('#ver-legados').textContent=state.legacy?'Voltar ao período':'Ver registros sem data';
}
const badge=status=>'<span class="badge '+(labels[status]?status:'')+'">'+esc(labels[status]||status||'Não informado')+'</span>';
function renderList(d) {
  state.pages=d.paginas;
  $('#titulo-lista').innerHTML=(state.legacy?'Chamados sem data':'Chamados do período')+' <span class="count">'+d.total+'</span>';
  $('#chamados').innerHTML=d.chamados.length?d.chamados.map(c=>'<tr><td><button class="open-call" data-id="'+esc(c.id)+'">#'+esc(c.id.slice(-6).toUpperCase())+'<small title="'+esc(c.descricao)+'">'+esc(c.descricao)+'</small></button></td><td>'+esc(c.tecnico||'Não atribuído')+'</td><td>'+badge(c.status)+'</td><td>'+stamp(c.dataAbertura)+'</td><td><button class="arrow" data-id="'+esc(c.id)+'" aria-label="Abrir chamado '+esc(c.id)+'">›</button></td></tr>').join(''):'<tr><td colspan="5" class="empty">Nenhum chamado encontrado. Ajuste os filtros ou o período.</td></tr>';
  $('#paginacao-texto').textContent=d.total?((d.pagina-1)*d.limite+1)+'–'+Math.min(d.pagina*d.limite,d.total)+' de '+d.total+' chamados':'0 chamados';
  $('#pagina').textContent=d.pagina+' / '+Math.max(1,d.paginas);
  $('#anterior').disabled=d.pagina<=1;$('#proxima').disabled=d.pagina>=d.paginas;
}
function renderDetail(c) {
  state.detail=c;
  $('#detalhe-titulo').textContent='Chamado #'+c.id.slice(-6).toUpperCase();
  const next={ABERTO:'EM_ANDAMENTO',EM_ANDAMENTO:'CONCLUIDO'}[c.status];
  $('#detalhe-corpo').innerHTML='<div class="detail-meta"><div><span>STATUS</span>'+badge(c.status)+'</div><div><span>TIPO</span>'+esc(c.tipo||'Não informado')+'</div><div><span>TÉCNICO</span>'+esc(c.tecnico||'Não atribuído')+'</div><div><span>USUÁRIO</span>'+esc(c.usuario||'Não informado')+'</div><div><span>ABERTURA</span>'+stamp(c.dataAbertura)+'</div><div><span>CONCLUSÃO</span>'+stamp(c.dataConclusao)+'</div></div><p class="description">'+esc(c.descricao)+'</p><div class="detail-actions">'+(next?'<button class="button primary" id="avancar" data-status="'+next+'">'+(next==='EM_ANDAMENTO'?'Iniciar atendimento':'Concluir chamado')+'</button>':'')+'<button class="button secondary" id="editar">Editar dados</button></div><h3>Considerações técnicas</h3><div class="notes">'+(c.consideracoes.length?c.consideracoes.map(n=>'<div class="note">'+esc(n.texto)+'<small>'+esc(n.autor)+' · '+stamp(n.data)+'</small></div>').join(''):'<p class="empty">Nenhuma consideração registrada.</p>')+'</div><h3>Histórico</h3><ol class="history">'+(c.historico.length?c.historico.map(h=>'<li>'+esc(h.descricao)+'<time>'+stamp(h.data)+'</time></li>').join(''):'<li>Registro anterior à inclusão do histórico.</li>')+'</ol><form id="form-nota"><label>Registrar consideração<textarea name="texto" maxlength="2000" required rows="3" placeholder="Descreva a análise e as ações realizadas"></textarea></label><button class="button secondary" type="submit">Salvar consideração</button></form><p class="form-error" id="detalhe-erro" role="alert" hidden></p>';
}
async function detail(id) {
  if(!$('#detalhes').open) $('#detalhes').showModal();
  $('#detalhe-corpo').innerHTML='<p class="empty">Carregando detalhes…</p>';
  try {renderDetail(await api('/api/chamados/'+encodeURIComponent(id)));}
  catch(error) {$('#detalhe-corpo').innerHTML='<p class="form-error">'+esc(error.message)+'</p>';}
}
async function update(path,method,body) {
  const c=state.detail;
  $('#detalhe-corpo').querySelectorAll('button').forEach(b=>{b.disabled=true;});
  try {
    renderDetail(await api('/api/chamados/'+c.id+path,{method,body:JSON.stringify({...body,versao:c.versao})}));
    notify('Chamado atualizado.');await load();
  } catch(error) {
    if(error.status===409) {try{renderDetail(await api('/api/chamados/'+c.id));}catch{}}
    $('#detalhe-erro').textContent=error.message;$('#detalhe-erro').hidden=false;
  } finally {$('#detalhe-corpo').querySelectorAll('button').forEach(b=>{b.disabled=false;});}
}
function openForm(edit=null) {
  state.edit=edit;$('#form-chamado').reset();$('#cadastro-erro').hidden=true;
  $('#cadastro-titulo').textContent=edit?'Editar chamado':'Novo chamado';
  $('#salvar-chamado').textContent=edit?'Salvar alterações':'Registrar chamado';
  if(edit) for(const name of ['descricao','tipo','tecnico','usuario']) $('#form-chamado [name="'+name+'"]').value=edit[name]||'';
  $('#cadastro').showModal();
}
$('#filtros').addEventListener('submit',e=>{e.preventDefault();state.filter=filters();state.legacy=false;load(true);});
$('#agrupamento').addEventListener('change',()=>{state.filter=filters();load(true);});
$('#pesquisa').addEventListener('submit',e=>{e.preventDefault();state.filter=filters();load(true);});
$('#busca').addEventListener('search',()=>{state.filter=filters();load(true);});
document.querySelectorAll('[data-dias]').forEach(b=>b.addEventListener('click',()=>{period(Number(b.dataset.dias));state.filter=filters();state.legacy=false;load(true);}));
for(const key of ['#inicio','#fim']) $(key).addEventListener('input',()=>document.querySelectorAll('[data-dias]').forEach(b=>b.classList.remove('selected')));
$('#limpar').addEventListener('click',()=>{$('#filtros').reset();$('#busca').value='';period(30);state.filter=filters();state.legacy=false;load(true);});
$('#atualizar').addEventListener('click',()=>load());$('#tentar').addEventListener('click',()=>load());
$('#ver-legados').addEventListener('click',()=>{state.legacy=!state.legacy;load(true);});
$('#anterior').addEventListener('click',()=>{if(state.page>1){state.page--;load();}});
$('#proxima').addEventListener('click',()=>{if(state.page<state.pages){state.page++;load();}});
$('#novo').addEventListener('click',()=>openForm());
document.addEventListener('click',e=>{
  const button=e.target.closest('button');
  if(!button||button.disabled)return;
  if(button.classList.contains('close'))button.closest('dialog').close();
  if(button.dataset.id)detail(button.dataset.id);
  if(button.id==='avancar')update('/status','PATCH',{status:button.dataset.status});
  if(button.id==='editar'){ $('#detalhes').close();openForm(state.detail); }
});
$('#detalhe-corpo').addEventListener('submit',e=>{if(e.target.id==='form-nota'){e.preventDefault();update('/consideracoes','POST',{texto:new FormData(e.target).get('texto')});}});
$('#form-chamado').addEventListener('submit',async e=>{
  e.preventDefault();const data=Object.fromEntries(new FormData(e.target));const editing=state.edit;
  if(editing){data.status=editing.status;data.versao=editing.versao;}
  $('#salvar-chamado').disabled=true;
  try {
    const result=await api('/api/chamados'+(editing?'/'+editing.id:''),{method:editing?'PUT':'POST',body:JSON.stringify(data)});
    $('#cadastro').close();notify(editing?'Dados atualizados.':'Chamado registrado.');
    await options();await load(true);await detail(result.id);
  }catch(error){$('#cadastro-erro').textContent=error.message;$('#cadastro-erro').hidden=false;}
  finally{$('#salvar-chamado').disabled=false;}
});
$('#hoje').textContent=new Intl.DateTimeFormat('pt-BR',{timeZone:'America/Sao_Paulo',day:'numeric',month:'short',year:'numeric'}).format(new Date());
period(30);await load();
setInterval(()=>{if(!document.hidden&&!document.querySelector('dialog[open]')&&!state.loading)load();},60000);
