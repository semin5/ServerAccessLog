(function () {
    const selectAll = document.getElementById("selectAllRows");
    const rowChecks = Array.from(document.querySelectorAll(".row-check"));

    function refreshSelectAll() {
        const checkedCount = rowChecks.filter((checkbox) => checkbox.checked).length;
        selectAll.checked = rowChecks.length > 0 && checkedCount === rowChecks.length;
        selectAll.indeterminate = checkedCount > 0 && checkedCount < rowChecks.length;
    }

    if (selectAll) {
        selectAll.addEventListener("change", function () {
            rowChecks.forEach((checkbox) => {
                checkbox.checked = selectAll.checked;
            });
            refreshSelectAll();
        });

        rowChecks.forEach((checkbox) => {
            checkbox.addEventListener("change", refreshSelectAll);
        });

        refreshSelectAll();
    }

    function closeModal(modal) {
        modal.classList.remove("is-open");
        modal.setAttribute("aria-hidden", "true");
    }

    document.querySelectorAll("[data-modal-target]").forEach((button) => {
        button.addEventListener("click", function () {
            const modal = document.getElementById(button.dataset.modalTarget);
            if (!modal) {
                return;
            }
            modal.classList.add("is-open");
            modal.setAttribute("aria-hidden", "false");
        });
    });

    document.querySelectorAll("[data-modal-close]").forEach((button) => {
        button.addEventListener("click", function () {
            const modal = button.closest(".visitor-modal");
            if (modal) {
                closeModal(modal);
            }
        });
    });

    window.addEventListener("keydown", function (event) {
        if (event.key !== "Escape") {
            return;
        }
        document.querySelectorAll(".visitor-modal.is-open").forEach(closeModal);
    });

    const ledgerPrintButton = document.getElementById("openLedgerPrintModal");
    const ledgerPrintModal = document.getElementById("ledgerPrintModal");
    const ledgerYearPicker = document.getElementById("ledgerYearPicker");
    const ledgerYearInput = document.getElementById("ledgerYearInput");
    const printDateToggle = document.getElementById("printDateToggle");
    const printDateField = document.getElementById("printDateField");
    const printDatePicker = document.getElementById("printDatePicker");
    const printDateEnabledInput = document.getElementById("printDateEnabledInput");
    const printDateInput = document.getElementById("printDateInput");
    const confirmLedgerPrint = document.getElementById("confirmLedgerPrint");
    const pdfForm = document.getElementById("pdfForm");

    ledgerPrintButton?.addEventListener("click", function () {
        if (!ledgerPrintModal) {
            return;
        }
        ledgerPrintModal.classList.add("is-open");
        ledgerPrintModal.setAttribute("aria-hidden", "false");
        ledgerYearPicker?.focus();
    });

    function syncPrintDatePicker() {
        if (!printDateToggle || !printDatePicker) {
            return;
        }
        const enabled = printDateToggle.checked;
        printDatePicker.disabled = !enabled;
        printDateField?.classList.toggle("is-visible", enabled);
    }

    printDateToggle?.addEventListener("change", syncPrintDatePicker);
    syncPrintDatePicker();

    confirmLedgerPrint?.addEventListener("click", function () {
        const year = ledgerYearPicker?.value || new Date().getFullYear().toString();
        if (ledgerYearInput) {
            ledgerYearInput.value = year;
        }
        const enabled = !!printDateToggle?.checked;
        if (printDateEnabledInput) {
            printDateEnabledInput.value = enabled ? "true" : "false";
        }
        if (printDateInput && printDatePicker) {
            printDateInput.value = printDatePicker.value || new Date().toISOString().slice(0, 10);
        }
        if (ledgerPrintModal) {
            closeModal(ledgerPrintModal);
        }
        pdfForm?.submit();
    });
})();
