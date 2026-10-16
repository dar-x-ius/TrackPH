/**
 * Shared helper for loading Leaflet + OpenStreetMap on demand. No API key, no billing account —
 * tiles come from the free OSM tile server, so pages just call TrackPHMaps.load() and get the
 * Leaflet `L` global back once it (and the heat-map plugin) are on the page.
 */
const TrackPHMaps = (function () {
    const LEAFLET_VERSION = '1.9.4';
    const HEAT_VERSION = '0.2.0';
    let loadPromise = null;

    function loadScript(src) {
        return new Promise((resolve, reject) => {
            const script = document.createElement('script');
            script.src = src;
            script.onload = resolve;
            script.onerror = () => reject(new Error('Failed to load ' + src));
            document.head.appendChild(script);
        });
    }

    function loadStylesheet(href) {
        const link = document.createElement('link');
        link.rel = 'stylesheet';
        link.href = href;
        document.head.appendChild(link);
    }

    function load() {
        if (loadPromise) return loadPromise;
        loadStylesheet('https://unpkg.com/leaflet@' + LEAFLET_VERSION + '/dist/leaflet.css');
        loadPromise = loadScript('https://unpkg.com/leaflet@' + LEAFLET_VERSION + '/dist/leaflet.js')
            .then(() => loadScript('https://unpkg.com/leaflet.heat@' + HEAT_VERSION + '/dist/leaflet-heat.js'))
            .then(() => window.L);
        return loadPromise;
    }

    function baseLayer() {
        return L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
            maxZoom: 18,
            attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
        });
    }

    /** Renders a "map unavailable" placeholder into the given element (e.g. the CDN couldn't be reached). */
    function unavailable(el) {
        el.innerHTML = '<div class="empty-state"><p>Map unavailable right now &mdash; couldn\'t load the map library.</p></div>';
    }

    /**
     * Small click-to-place map for picking a project's exact coordinates. Starts centered on the
     * Philippines; clicking anywhere drops (or moves) a single marker and reports its new position.
     */
    function initPinPicker(elId, initial, onChange) {
        const el = document.getElementById(elId);
        load().then(L => {
            const center = initial && initial.lat != null ? [initial.lat, initial.lng] : [12.8797, 121.7740];
            const map = L.map(el).setView(center, initial && initial.lat != null ? 13 : 6);
            baseLayer().addTo(map);

            let marker = initial && initial.lat != null ? L.marker(center, { draggable: true }).addTo(map) : null;
            if (marker) marker.on('dragend', () => { const p = marker.getLatLng(); onChange(p.lat, p.lng); });

            map.on('click', e => {
                if (marker) {
                    marker.setLatLng(e.latlng);
                } else {
                    marker = L.marker(e.latlng, { draggable: true }).addTo(map);
                    marker.on('dragend', () => { const p = marker.getLatLng(); onChange(p.lat, p.lng); });
                }
                onChange(e.latlng.lat, e.latlng.lng);
            });
        }).catch(() => unavailable(el));
    }

    return { load, unavailable, initPinPicker, baseLayer };
})();
