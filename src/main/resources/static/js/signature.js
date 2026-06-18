(function () {
    const canvas = document.getElementById("signatureCanvas");
    const hiddenInput = document.getElementById("signatureData");
    const clearButton = document.getElementById("clearSignature");
    const fileInput = document.querySelector('input[type="file"][name="signatureFile"]');
    const form = document.querySelector("form.entry-form");
    const privacyCheckbox = document.querySelector('input[name="privacyAgreed"]');
    const submitButton = document.getElementById("submitEntry") || form?.querySelector('button[type="submit"]');
    if (!canvas || !hiddenInput || !form) {
        return;
    }

    const context = canvas.getContext("2d");
    let drawing = false;
    let dirty = false;

    function resizeCanvas() {
        const rect = canvas.getBoundingClientRect();
        const snapshot = context.getImageData(0, 0, canvas.width, canvas.height);
        canvas.width = Math.max(600, Math.floor(rect.width * window.devicePixelRatio));
        canvas.height = Math.max(220, Math.floor(rect.height * window.devicePixelRatio));
        context.putImageData(snapshot, 0, 0);
        context.lineWidth = 4 * window.devicePixelRatio;
        context.lineCap = "round";
        context.lineJoin = "round";
        context.strokeStyle = "#111827";
    }

    function point(event) {
        const rect = canvas.getBoundingClientRect();
        const touch = event.touches ? event.touches[0] : event;
        return {
            x: (touch.clientX - rect.left) * (canvas.width / rect.width),
            y: (touch.clientY - rect.top) * (canvas.height / rect.height)
        };
    }

    function start(event) {
        event.preventDefault();
        drawing = true;
        dirty = true;
        const p = point(event);
        context.beginPath();
        context.moveTo(p.x, p.y);
    }

    function move(event) {
        if (!drawing) {
            return;
        }
        event.preventDefault();
        const p = point(event);
        context.lineTo(p.x, p.y);
        context.stroke();
    }

    function stop() {
        drawing = false;
    }

    function clear() {
        context.clearRect(0, 0, canvas.width, canvas.height);
        dirty = false;
        hiddenInput.value = "";
    }

    function syncSubmitButton() {
        if (!privacyCheckbox || !submitButton) {
            return;
        }
        const agreed = privacyCheckbox.checked;
        submitButton.disabled = !agreed;
        submitButton.classList.toggle("is-ready", agreed);
    }

    canvas.addEventListener("mousedown", start);
    canvas.addEventListener("mousemove", move);
    window.addEventListener("mouseup", stop);
    canvas.addEventListener("touchstart", start, {passive: false});
    canvas.addEventListener("touchmove", move, {passive: false});
    window.addEventListener("touchend", stop);
    clearButton?.addEventListener("click", clear);
    fileInput?.addEventListener("change", function () {
        if (fileInput.files.length > 0) {
            clear();
        }
    });
    privacyCheckbox?.addEventListener("change", syncSubmitButton);
    form.addEventListener("submit", function () {
        if (dirty && (!fileInput || fileInput.files.length === 0)) {
            hiddenInput.value = canvas.toDataURL("image/png");
        }
    });

    resizeCanvas();
    syncSubmitButton();
})();
