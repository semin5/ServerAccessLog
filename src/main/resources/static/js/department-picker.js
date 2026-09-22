(() => {
    const select = document.getElementById('recipientDepartment');
    if (!select) return;
    const field = select.closest('.department-field');
    const trigger = document.createElement('button');
    trigger.type = 'button';
    trigger.className = 'department-picker-trigger';
    trigger.setAttribute('aria-label', '부서 선택');
    trigger.setAttribute('aria-expanded', 'false');
    const menu = document.createElement('div');
    menu.className = 'department-picker-menu';
    menu.hidden = true;
    const search = document.createElement('input');
    search.type = 'search';
    search.className = 'department-picker-search';
    search.placeholder = '부서 검색';
    search.setAttribute('aria-label', '부서 검색');
    const results = document.createElement('div');
    menu.append(search, results);
    select.after(trigger, menu);
    select.classList.add('department-picker-native');

    const options = [...select.options].filter(option => option.value).map(option => ({
        name: option.value,
        middleCategory: option.dataset.middleCategory?.trim() || ''
    }));
    const updateLabel = () => { trigger.textContent = select.value || '부서 선택'; };
    const close = () => { menu.hidden = true; trigger.setAttribute('aria-expanded', 'false'); };
    const choose = item => {
        select.value = item.name;
        select.dispatchEvent(new Event('change', { bubbles: true }));
        updateLabel();
        close();
    };
    const makeOption = item => {
        const button = document.createElement('button');
        button.type = 'button';
        button.className = 'department-picker-option';
        button.textContent = item.name;
        if (select.value === item.name) button.classList.add('selected');
        button.addEventListener('click', () => choose(item));
        return button;
    };
    const render = () => {
        results.replaceChildren();
        const query = search.value.trim().toLocaleLowerCase('ko');
        const visible = options.filter(item => !query || item.name.toLocaleLowerCase('ko').includes(query) || item.middleCategory.toLocaleLowerCase('ko').includes(query));
        const groups = new Map();
        visible.forEach(item => {
            if (!item.middleCategory) results.append(makeOption(item));
            else {
                if (!groups.has(item.middleCategory)) groups.set(item.middleCategory, []);
                groups.get(item.middleCategory).push(item);
            }
        });
        [...groups].sort(([a], [b]) => a.localeCompare(b, 'ko')).forEach(([category, members]) => {
            const group = document.createElement('details');
            group.className = 'department-picker-group';
            group.open = Boolean(query);
            const heading = document.createElement('summary');
            heading.textContent = category;
            group.append(heading, ...members.map(makeOption));
            results.append(group);
        });
        if (!visible.length) {
            const empty = document.createElement('p');
            empty.className = 'department-picker-empty';
            empty.textContent = '검색 결과가 없습니다.';
            results.append(empty);
        }
    };
    trigger.addEventListener('click', () => {
        if (!menu.hidden) { close(); return; }
        search.value = '';
        render();
        menu.hidden = false;
        trigger.setAttribute('aria-expanded', 'true');
        search.focus();
    });
    search.addEventListener('input', render);
    search.addEventListener('keydown', event => { if (event.key === 'Enter') event.preventDefault(); });
    select.addEventListener('invalid', event => { event.preventDefault(); if (menu.hidden) trigger.click(); });
    field.querySelector('label[for="recipientDepartment"]')?.addEventListener('click', event => { event.preventDefault(); trigger.click(); });
    document.addEventListener('pointerdown', event => { if (!field.contains(event.target)) close(); });
    document.addEventListener('keydown', event => { if (event.key === 'Escape') close(); });
    updateLabel();
})();
