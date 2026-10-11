/**
 * TrackPH click-to-sort tables. Give a <th> a data-sort="fieldName" attribute and call
 * makeSortable(table, getRows, renderRows) once the table's rows exist. Clicking a header
 * toggles ascending/descending on that field and re-renders via the page's own render function.
 */
window.makeSortable = function (table, getRows, renderRows) {
    const headers = table.querySelectorAll('thead th[data-sort]');
    let sortKey = null;
    let sortAsc = true;

    function compare(a, b) {
        let av = a[sortKey];
        let bv = b[sortKey];
        if (av == null) av = '';
        if (bv == null) bv = '';
        if (typeof av === 'string') av = av.toLowerCase();
        if (typeof bv === 'string') bv = bv.toLowerCase();
        if (av < bv) return sortAsc ? -1 : 1;
        if (av > bv) return sortAsc ? 1 : -1;
        return 0;
    }

    headers.forEach(th => {
        th.classList.add('sortable');
        th.addEventListener('click', () => {
            const key = th.dataset.sort;
            sortAsc = sortKey === key ? !sortAsc : true;
            sortKey = key;
            headers.forEach(h => h.setAttribute('aria-sort', h === th ? (sortAsc ? 'ascending' : 'descending') : 'none'));
            renderRows(getRows().slice().sort(compare));
        });
    });
};
