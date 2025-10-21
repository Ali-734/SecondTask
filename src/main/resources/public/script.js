const form = document.getElementById('uploadForm');
const fileInput = document.getElementById('fileInput');
const statusEl = document.getElementById('status');
const linkEl = document.getElementById('link');
const filesListEl = document.getElementById('filesList');
const authMessageEl = document.getElementById('authMessage');
const statsContentEl = document.getElementById('statsContent');

let currentFiles = [];

async function refreshFilesList(){
  console.log('Загрузка списка файлов...');
  try{
    const r = await fetch('/api/files');
    if(!r.ok) {
      console.error('Ошибка загрузки списка файлов:', r.status);
      filesListEl.innerHTML = '<div style="text-align: center; color: #dc3545; padding: 20px;">Ошибка загрузки списка файлов</div>';
      return;
    }
    const j = await r.json();
    console.log('Получены файлы:', j.files);
    currentFiles = j.files || [];
    renderFilesList(currentFiles);
  }catch(e){
    console.error('Ошибка загрузки списка файлов:', e);
    filesListEl.innerHTML = '<div style="text-align: center; color: #dc3545; padding: 20px;">Ошибка загрузки списка файлов</div>';
  }
}

async function refreshFilesListAndStats(){
  await refreshFilesList();
  await refreshDetailedStats();
}

function renderFilesList(files) {
  if (files && files.length > 0) {
    filesListEl.innerHTML = files.map(file => `
      <div class="file-item" id="file-${file.token}" style="display: flex; justify-content: space-between; align-items: center; padding: 12px; margin: 10px 0; background: #fff; border-radius: 8px; border: 1px solid #e9ecef;">
        <div style="flex: 1;">
          <div class="name">${file.name || 'Без имени'}</div>
          <div class="meta">
            Размер: ${formatFileSize(file.size)} | 
            Скачиваний: <span id="downloads-${file.token}">${file.downloads}</span> | 
            Загружен: ${formatDate(file.created)} | 
            Последнее скачивание: ${file.lastDownloaded > 0 ? formatDate(file.lastDownloaded) : 'Никогда'}
          </div>
        </div>
        <div class="file-actions">
          <button onclick="copyFileLink('${file.token}')" class="btn btn-success">Копировать ссылку</button>
          <button onclick="downloadFile('${file.token}')" class="btn btn-primary">Скачать</button>
          <button onclick="deleteFile('${file.token}')" class="btn btn-danger">Удалить</button>
        </div>
      </div>
    `).join('');
  } else {
    filesListEl.innerHTML = '<div style="text-align: center; color: #666; padding: 20px;">Нет загруженных файлов</div>';
  }
}

function applySorting() {
  const sortBy = document.getElementById('sortSelect').value;
  const sortOrder = document.getElementById('sortOrder').value;
  let sortedFiles = [...currentFiles];
  
  if (sortOrder === 'default') {
    renderFilesList(sortedFiles);
    return;
  }
  
  let comparison = 0;
  switch(sortBy) {
    case 'name':
      sortedFiles.sort((a, b) => {
        comparison = (a.name || '').localeCompare(b.name || '');
        return sortOrder === 'asc' ? comparison : -comparison;
      });
      break;
    case 'size':
      sortedFiles.sort((a, b) => {
        comparison = a.size - b.size;
        return sortOrder === 'asc' ? comparison : -comparison;
      });
      break;
    case 'downloads':
      sortedFiles.sort((a, b) => {
        comparison = a.downloads - b.downloads;
        return sortOrder === 'asc' ? comparison : -comparison;
      });
      break;
    case 'date':
      sortedFiles.sort((a, b) => {
        comparison = a.created - b.created;
        return sortOrder === 'asc' ? comparison : -comparison;
      });
      break;
    case 'lastDownload':
      sortedFiles.sort((a, b) => {
        comparison = a.lastDownloaded - b.lastDownloaded;
        return sortOrder === 'asc' ? comparison : -comparison;
      });
      break;
  }
  
  renderFilesList(sortedFiles);
}

function formatFileSize(bytes) {
  if (bytes === 0) return '0 B';
  const k = 1024;
  const sizes = ['B', 'KB', 'MB', 'GB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
}

function formatDate(timestamp) {
  const date = new Date(timestamp);
  return date.toLocaleString('ru-RU');
}

function formatMaybeDate(timestamp){
  if(!timestamp || timestamp <= 0){
    return '-';
  }
  return formatDate(timestamp);
}

function copyFileLink(token) {
  const url = `${window.location.origin}/d/${token}`;
  navigator.clipboard.writeText(url).then(() => {
    alert('Ссылка скопирована в буфер обмена!');
  }).catch(() => {
    alert('Не удалось скопировать ссылку');
  });
}

function downloadFile(token) {
  const url = `${window.location.origin}/d/${token}`;
  window.open(url, '_self');
  setTimeout(async () => {
    await updateDownloadCount(token);
    await refreshFilesListAndStats();
  }, 1000);
}

async function deleteFile(token) {
  if (!confirm('Вы уверены, что хотите удалить этот файл?')) {
    return;
  }
  
  try {
    const response = await fetch(`/api/delete/${token}`, {
      method: 'DELETE'
    });
    
    if (response.ok) {
      // Обновляем все данные
      await refreshFilesListAndStats();
    } else {
      alert('Ошибка при удалении файла');
    }
  } catch (error) {
    alert('Ошибка при удалении файла');
  }
}

async function updateDownloadCount(token) {
  try {
    const response = await fetch('/api/files');
    if (response.ok) {
      const data = await response.json();
      const file = data.files.find(f => f.token === token);
      if (file) {
        const downloadsElement = document.getElementById(`downloads-${token}`);
        if (downloadsElement) {
          downloadsElement.textContent = file.downloads;
        }
      }
    }
  } catch (error) {
    console.error('Ошибка обновления счетчика скачиваний:', error);
  }
}

async function checkAuthStatus() {
  authMessageEl.classList.add('hidden');
}

async function refreshDetailedStats() {
  console.log('Загрузка детальной статистики...');
  try {
    const response = await fetch('/api/file-stats');
    if (!response.ok) {
      console.error('Ошибка загрузки детальной статистики:', response.status);
      statsContentEl.innerHTML = '<div style="text-align: center; color: #dc3545;">Ошибка загрузки детальной статистики</div>';
      return;
    }
    const stats = await response.json();
    console.log('Получена статистика:', stats);
    
    statsContentEl.innerHTML = `
      <div class="stats-grid">
        <div class="card">
          <h4>Общая статистика</h4>
          <div class="metrics">
            <div>Всего файлов: <strong>${stats.totalFiles}</strong></div>
            <div>Общий размер: <strong>${formatFileSize(stats.totalSize)}</strong></div>
            <div>Всего скачиваний: <strong>${stats.totalDownloads}</strong></div>
          </div>
        </div>
        
        <div class="card">
          <h4>Размеры файлов</h4>
          <div class="metrics">
            <div>Максимум: <strong>${formatFileSize(stats.sizeStats.max)}</strong></div>
            <div>Минимум: <strong>${formatFileSize(stats.sizeStats.min)}</strong></div>
            <div>Медиана: <strong>${formatFileSize(stats.sizeStats.median)}</strong></div>
            <div>Среднее: <strong>${formatFileSize(stats.sizeStats.average)}</strong></div>
          </div>
        </div>
        
        <div class="card">
          <h4>Скачивания</h4>
          <div class="metrics">
            <div>Максимум: <strong>${stats.downloadStats.max}</strong></div>
            <div>Минимум: <strong>${stats.downloadStats.min}</strong></div>
            <div>Медиана: <strong>${stats.downloadStats.median}</strong></div>
            <div>Среднее: <strong>${Math.round(stats.downloadStats.average)}</strong></div>
          </div>
        </div>
        
        <div class="card time-card">
          <h4>Время</h4>
          <div class="metrics">
            <div>Самый старый: <strong>${formatMaybeDate(stats.timeStats.oldest)}</strong></div>
            <div>Самый новый: <strong>${formatMaybeDate(stats.timeStats.newest)}</strong></div>
          </div>
        </div>
      </div>
      
      <div class="card">
        <h4>Статистика по форматам</h4>
        <div class="chips">
          ${stats.formatStats.map(format => `
            <div class="chip">
              <div style="font-weight:600;color:#0d6efd;">.${format.format}</div>
              <div class="metrics">
                ${format.count} файл(ов)<br>
                ${formatFileSize(format.size)}
              </div>
            </div>
          `).join('')}
        </div>
      </div>
    `;
  } catch (error) {
    statsContentEl.innerHTML = '<div style="text-align: center; color: #dc3545;">Ошибка загрузки детальной статистики</div>';
  }
}

function formatAge(seconds) {
  const days = Math.floor(seconds / 86400);
  const hours = Math.floor((seconds % 86400) / 3600);
  const minutes = Math.floor((seconds % 3600) / 60);
  
  if (days > 0) return `${days}д ${hours}ч`;
  if (hours > 0) return `${hours}ч ${minutes}м`;
  return `${minutes}м`;
}

refreshFilesListAndStats();
checkAuthStatus();

form.addEventListener('submit', async (e)=>{
  e.preventDefault();
  const f = fileInput.files[0];
  if(!f){
    alert('Выберите файл');
    return;
  }
  statusEl.textContent = 'Загрузка...';
  linkEl.classList.add('hidden');

  const fd = new FormData();
  fd.append('file', f, f.name);

  const r = await fetch('/api/upload', { method:'POST', body: fd });
  if(!r.ok){
    statusEl.textContent = 'Ошибка загрузки';
    return;
  }
  const j = await r.json();
  statusEl.textContent = 'Готово';
  linkEl.innerHTML = `
    <div style="margin-bottom: 8px;">Ссылка для скачивания:</div>
    <div style="display: flex; gap: 8px; align-items: center;">
      <input type="text" value="${j.url}" readonly style="flex: 1; padding: 4px; border: 1px solid #ccc; border-radius: 4px; font-family: monospace;">
      <button onclick="navigator.clipboard.writeText('${j.url}')" style="padding: 4px 8px; background: #28a745; color: white; border: none; border-radius: 4px; cursor: pointer;">Копировать</button>
    </div>
    <div style="margin-top: 8px;">
      <a href="${j.url}" target="_self" style="color: #0064e0; text-decoration: none;">Открыть ссылку</a>
    </div>
  `;
  linkEl.classList.remove('hidden');
  await refreshFilesListAndStats();
});