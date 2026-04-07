(function () {
    const forms = document.querySelectorAll('[data-live-search-form]');

    if (!forms.length) {
        return;
    }

    forms.forEach((form) => {
        const root = form.closest('.card');
        const input = form.querySelector('[data-live-search-input]');
        const toggle = form.querySelector('[data-live-search-toggle]');
        const clearButton = form.querySelector('[data-live-search-clear]');
        const caseSensitiveField = form.querySelector('[data-live-search-case-sensitive]');
        const results = root ? root.querySelector('[data-live-search-results]') : null;

        if (!input || !toggle || !clearButton || !caseSensitiveField || !results) {
            return;
        }

        let timerId = null;
        let activeRequest = null;
        let requestVersion = 0;

        const syncToggleState = () => {
            const caseSensitive = '1' === caseSensitiveField.value;
            toggle.classList.toggle('active', caseSensitive);
            toggle.setAttribute('aria-pressed', caseSensitive ? 'true' : 'false');
            toggle.textContent = caseSensitive ? 'Case Sensitive: On' : 'Case Sensitive: Off';
        };

        const buildUrl = () => {
            const url = new URL(form.action, window.location.origin);
            const searchTerm = input.value.trim();

            if (searchTerm) {
                url.searchParams.set('q', searchTerm);
            } else {
                url.searchParams.delete('q');
            }

            if ('1' === caseSensitiveField.value) {
                url.searchParams.set('case_sensitive', '1');
            } else {
                url.searchParams.delete('case_sensitive');
            }

            return url;
        };

        const renderResults = async () => {
            if (activeRequest) {
                activeRequest.abort();
            }

            const controller = new AbortController();
            activeRequest = controller;
            requestVersion += 1;
            const currentRequestVersion = requestVersion;

            const url = buildUrl();

            try {
                const response = await fetch(url.toString(), {
                    headers: {
                        'X-Requested-With': 'XMLHttpRequest',
                    },
                    signal: controller.signal,
                });

                if (!response.ok) {
                    return;
                }

                const html = await response.text();

                if (currentRequestVersion !== requestVersion) {
                    return;
                }

                results.innerHTML = html;
                window.history.replaceState({}, '', url.pathname + url.search);
            } catch (error) {
                if (error.name !== 'AbortError') {
                    console.error('Live search failed.', error);
                }
            } finally {
                if (activeRequest === controller) {
                    activeRequest = null;
                }
            }
        };

        const queueSearch = () => {
            window.clearTimeout(timerId);
            timerId = window.setTimeout(renderResults, 220);
        };

        syncToggleState();

        form.addEventListener('submit', (event) => {
            event.preventDefault();
            renderResults();
        });

        input.addEventListener('input', queueSearch);

        toggle.addEventListener('click', () => {
            caseSensitiveField.value = '1' === caseSensitiveField.value ? '0' : '1';
            syncToggleState();
            renderResults();
        });

        clearButton.addEventListener('click', () => {
            input.value = '';
            input.focus();
            renderResults();
        });
    });
})();
