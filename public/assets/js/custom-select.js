(function () {
    const SELECTOR = 'select:not([multiple]):not([data-custom-select-ready])';

    const closeAll = (exceptRoot) => {
        document.querySelectorAll('.custom-select-root.is-open').forEach((root) => {
            if (root !== exceptRoot) {
                root.classList.remove('is-open');
            }
        });
    };

    const createOptionButton = (select, option, list, trigger) => {
        const item = document.createElement('button');
        item.type = 'button';
        item.className = 'custom-select-option';
        item.dataset.value = option.value;
        item.textContent = option.textContent;
        item.disabled = option.disabled;

        const syncSelectedState = () => {
            const isSelected = select.value === option.value;
            item.classList.toggle('is-selected', isSelected);
            item.setAttribute('aria-selected', isSelected ? 'true' : 'false');
        };

        item.addEventListener('click', () => {
            if (option.disabled) {
                return;
            }

            select.value = option.value;
            trigger.firstChild.textContent = option.textContent;
            closeAll();
            select.dispatchEvent(new Event('change', { bubbles: true }));
            syncSelectedState();
            list.querySelectorAll('.custom-select-option').forEach((button) => {
                if (button !== item) {
                    button.classList.remove('is-selected');
                    button.setAttribute('aria-selected', 'false');
                }
            });
            trigger.focus();
        });

        syncSelectedState();

        return item;
    };

    const buildCustomSelect = (select) => {
        select.dataset.customSelectReady = 'true';

        const wrapper = document.createElement('div');
        wrapper.className = 'custom-select-root';

        const inheritedClasses = Array.from(select.classList).filter((className) => className !== 'form-control');
        if (inheritedClasses.length) {
            wrapper.classList.add(...inheritedClasses.map((className) => `custom-select-${className}`));
        }

        select.parentNode.insertBefore(wrapper, select);
        wrapper.appendChild(select);
        select.classList.add('custom-select-native');

        const trigger = document.createElement('button');
        trigger.type = 'button';
        trigger.className = 'custom-select-trigger';
        trigger.setAttribute('aria-haspopup', 'listbox');
        trigger.setAttribute('aria-expanded', 'false');

        const triggerLabel = document.createElement('span');
        triggerLabel.className = 'custom-select-trigger-label';
        trigger.appendChild(triggerLabel);

        const triggerIcon = document.createElement('span');
        triggerIcon.className = 'custom-select-trigger-icon';
        triggerIcon.setAttribute('aria-hidden', 'true');
        trigger.appendChild(triggerIcon);

        const menu = document.createElement('div');
        menu.className = 'custom-select-menu';

        const list = document.createElement('div');
        list.className = 'custom-select-list';
        list.setAttribute('role', 'listbox');
        menu.appendChild(list);

        wrapper.appendChild(trigger);
        wrapper.appendChild(menu);

        const syncFromSelect = () => {
            const selectedOption = select.options[select.selectedIndex] || select.options[0];
            triggerLabel.textContent = selectedOption ? selectedOption.textContent : '';
            trigger.disabled = select.disabled;
            wrapper.classList.toggle('is-disabled', !!select.disabled);

            list.querySelectorAll('.custom-select-option').forEach((button) => {
                const isSelected = button.dataset.value === select.value;
                button.classList.toggle('is-selected', isSelected);
                button.setAttribute('aria-selected', isSelected ? 'true' : 'false');
            });
        };

        Array.from(select.options).forEach((option) => {
            list.appendChild(createOptionButton(select, option, list, trigger));
        });

        trigger.addEventListener('click', () => {
            if (select.disabled) {
                return;
            }

            const willOpen = !wrapper.classList.contains('is-open');
            closeAll(wrapper);
            wrapper.classList.toggle('is-open', willOpen);
            trigger.setAttribute('aria-expanded', willOpen ? 'true' : 'false');
        });

        trigger.addEventListener('keydown', (event) => {
            if (!['ArrowDown', 'ArrowUp', 'Enter', ' '].includes(event.key)) {
                if ('Escape' === event.key) {
                    wrapper.classList.remove('is-open');
                    trigger.setAttribute('aria-expanded', 'false');
                }
                return;
            }

            event.preventDefault();

            const enabledOptions = Array.from(select.options).filter((option) => !option.disabled);
            const currentIndex = enabledOptions.findIndex((option) => option.value === select.value);

            if ('ArrowDown' === event.key || 'ArrowUp' === event.key) {
                const direction = 'ArrowDown' === event.key ? 1 : -1;
                const nextIndex = currentIndex < 0
                    ? 0
                    : (currentIndex + direction + enabledOptions.length) % enabledOptions.length;

                select.value = enabledOptions[nextIndex].value;
                select.dispatchEvent(new Event('change', { bubbles: true }));
                syncFromSelect();
            } else {
                const isOpen = wrapper.classList.contains('is-open');
                closeAll(wrapper);
                wrapper.classList.toggle('is-open', !isOpen);
                trigger.setAttribute('aria-expanded', !isOpen ? 'true' : 'false');
            }
        });

        select.addEventListener('change', () => {
            syncFromSelect();
            trigger.setAttribute('aria-expanded', wrapper.classList.contains('is-open') ? 'true' : 'false');
        });

        syncFromSelect();
    };

    document.addEventListener('click', (event) => {
        if (!event.target.closest('.custom-select-root')) {
            closeAll();
        }
    });

    document.querySelectorAll(SELECTOR).forEach(buildCustomSelect);
})();
