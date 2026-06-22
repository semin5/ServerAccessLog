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
        if (modal.id === "confirmationPreviewModal") {
            const frame = document.getElementById("confirmationPreviewFrame");
            if (frame) {
                frame.removeAttribute("src");
            }
            if (currentPreviewObjectUrl) {
                URL.revokeObjectURL(currentPreviewObjectUrl);
                currentPreviewObjectUrl = "";
            }
        }
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

    const confirmationPreviewModal = document.getElementById("confirmationPreviewModal");
    const confirmationPreviewFrame = document.getElementById("confirmationPreviewFrame");
    const confirmationPreviewTitle = document.getElementById("confirmationPreviewTitle");
    const printConfirmationPdf = document.getElementById("printConfirmationPdf");
    let currentPreviewObjectUrl = "";

    document.querySelectorAll(".confirmation-preview-button").forEach((button) => {
        button.addEventListener("click", function () {
            if (!confirmationPreviewModal || !confirmationPreviewFrame) {
                return;
            }
            if (currentPreviewObjectUrl) {
                URL.revokeObjectURL(currentPreviewObjectUrl);
                currentPreviewObjectUrl = "";
            }
            if (confirmationPreviewTitle) {
                confirmationPreviewTitle.textContent = "확인서 미리보기";
            }
            confirmationPreviewFrame.src = button.dataset.pdfPreviewUrl;
            printConfirmationPdf?.setAttribute("data-pdf-print-url", button.dataset.pdfPrintUrl || button.dataset.pdfPreviewUrl);
            confirmationPreviewModal.classList.add("is-open");
            confirmationPreviewModal.setAttribute("aria-hidden", "false");
        });
    });

    printConfirmationPdf?.addEventListener("click", function () {
        if (!confirmationPreviewFrame?.src) {
            return;
        }
        try {
            confirmationPreviewFrame.contentWindow?.focus();
            confirmationPreviewFrame.contentWindow?.print();
        } catch (error) {
            window.open(printConfirmationPdf.dataset.pdfPrintUrl || confirmationPreviewFrame.src, "_blank", "noopener");
        }
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

    confirmLedgerPrint?.addEventListener("click", async function () {
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
        if (!pdfForm || !confirmationPreviewModal || !confirmationPreviewFrame) {
            return;
        }
        try {
            confirmLedgerPrint.disabled = true;
            const response = await fetch(pdfForm.action, {
                method: "POST",
                body: new FormData(pdfForm),
                credentials: "same-origin"
            });
            if (!response.ok) {
                throw new Error("PDF preview failed");
            }
            if (currentPreviewObjectUrl) {
                URL.revokeObjectURL(currentPreviewObjectUrl);
            }
            currentPreviewObjectUrl = URL.createObjectURL(await response.blob());
            if (confirmationPreviewTitle) {
                confirmationPreviewTitle.textContent = "관리대장 미리보기";
            }
            confirmationPreviewFrame.src = currentPreviewObjectUrl;
            printConfirmationPdf?.setAttribute("data-pdf-print-url", currentPreviewObjectUrl);
            confirmationPreviewModal.classList.add("is-open");
            confirmationPreviewModal.setAttribute("aria-hidden", "false");
        } catch (error) {
            alert("관리대장 미리보기를 만들 수 없습니다. 다시 시도해주세요.");
        } finally {
            confirmLedgerPrint.disabled = false;
        }
    });
})();
