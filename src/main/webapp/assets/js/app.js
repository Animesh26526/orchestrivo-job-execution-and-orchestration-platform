/**
 * ORCHESTRIVO - Stage 4 Client Interactions & Enhancements
 */

document.addEventListener('DOMContentLoaded', function () {
  // 1. Prevent accidental duplicate form submissions
  const forms = document.querySelectorAll('form[method="post"]:not(.no-double-submit-prevent)');
  forms.forEach(function (form) {
    form.addEventListener('submit', function (e) {
      const submitBtn = form.querySelector('button[type="submit"], input[type="submit"]');
      if (submitBtn && !submitBtn.disabled) {
        // Small delay so form submission actually fires in browser
        setTimeout(function () {
          submitBtn.disabled = true;
          if (submitBtn.tagName === 'BUTTON') {
            submitBtn.dataset.originalText = submitBtn.innerHTML;
            submitBtn.innerHTML = 'Processing...';
          }
        }, 10);
      }
    });
  });

  // 2. Algorithm select handler for Round Robin quantum
  const algoSelect = document.getElementById('algorithmName');
  if (algoSelect) {
    function updateQuantumVisibility() {
      const qGroup = document.getElementById('quantumGroup');
      if (qGroup) {
        if (algoSelect.value === 'ROUND_ROBIN') {
          qGroup.style.display = 'block';
        } else {
          qGroup.style.display = 'none';
        }
      }
    }
    algoSelect.addEventListener('change', updateQuantumVisibility);
    updateQuantumVisibility();
  }

  // 3. Accessible Dropdown Navigation
  const dropdownContainers = document.querySelectorAll('.has-dropdown');
  function closeAllDropdowns(exceptContainer) {
    dropdownContainers.forEach(function (container) {
      if (container !== exceptContainer) {
        container.classList.remove('open');
        const btn = container.querySelector('.dropdown-toggle');
        if (btn) btn.setAttribute('aria-expanded', 'false');
      }
    });
  }

  dropdownContainers.forEach(function (container) {
    const toggle = container.querySelector('.dropdown-toggle');
    const menu = container.querySelector('.dropdown-menu');
    if (!toggle || !menu) return;

    toggle.addEventListener('click', function (e) {
      e.stopPropagation();
      const isOpen = container.classList.contains('open');
      closeAllDropdowns(container);
      if (!isOpen) {
        container.classList.add('open');
        toggle.setAttribute('aria-expanded', 'true');
      } else {
        container.classList.remove('open');
        toggle.setAttribute('aria-expanded', 'false');
      }
    });

    container.addEventListener('keydown', function (e) {
      if (e.key === 'Escape') {
        container.classList.remove('open');
        toggle.setAttribute('aria-expanded', 'false');
        toggle.focus();
      } else if (e.key === 'ArrowDown' && container.classList.contains('open')) {
        const items = menu.querySelectorAll('.dropdown-item');
        if (items.length > 0) {
          e.preventDefault();
          items[0].focus();
        }
      }
    });
  });

  document.addEventListener('click', function (e) {
    if (!e.target.closest('.has-dropdown')) {
      closeAllDropdowns(null);
    }
  });

  // 4. Mobile menu toggle
  const mobileToggle = document.getElementById('navMobileToggle');
  const navMenu = document.getElementById('navLinks');
  if (mobileToggle && navMenu) {
    mobileToggle.addEventListener('click', function () {
      const isExpanded = navMenu.classList.toggle('open');
      mobileToggle.setAttribute('aria-expanded', isExpanded ? 'true' : 'false');
    });
  }
});

// Dynamic Process Table Helper
function addProcessRow(tableId, counterPrefix) {
  const table = document.getElementById(tableId);
  if (!table) return;
  const tbody = table.querySelector('tbody');
  if (!tbody) return;

  const currentCount = tbody.querySelectorAll('tr').length + 1;
  const tr = document.createElement('tr');
  const defaultId = (counterPrefix || 'P') + currentCount;

  tr.innerHTML = `
    <td><input type="text" name="processId" class="form-input" value="${defaultId}" required style="font-family: monospace; font-weight: 600;" /></td>
    <td><input type="number" name="arrivalTime" class="form-input" value="0" min="0" required /></td>
    <td><input type="number" name="burstTime" class="form-input" value="5" min="1" required /></td>
    <td class="actions-cell">
      <button type="button" class="btn btn-outline-danger btn-sm" onclick="removeProcessRow(this)">Remove</button>
    </td>
  `;
  tbody.appendChild(tr);
}

function removeProcessRow(btn) {
  const tr = btn.closest('tr');
  const tbody = tr ? tr.parentElement : null;
  if (tbody && tbody.querySelectorAll('tr').length <= 1) {
    alert('A workload must contain at least one process.');
    return;
  }
  if (tr) {
    tr.remove();
  }
}

// Background Job Status Poller
function startJobStatusPolling(jobId, contextPath, onCompleteCallback) {
  if (!jobId) return;

  const statusBadge = document.getElementById('jobStatusBadge');
  const progressText = document.getElementById('jobProgressText');
  const resultContainer = document.getElementById('jobResultContainer');

  const intervalId = setInterval(function () {
    fetch(contextPath + '/jobs/status?id=' + encodeURIComponent(jobId))
      .then(function (res) {
        if (!res.ok) throw new Error('Status poll failed');
        return res.json();
      })
      .then(function (data) {
        if (statusBadge && data.status) {
          statusBadge.textContent = data.status;
          statusBadge.className = 'badge ' + getBadgeClass(data.status);
        }
        if (progressText && data.attemptCount !== undefined) {
          progressText.textContent = 'Attempt ' + data.attemptCount + ' of ' + (data.maxAttempts || 3);
        }

        if (data.status === 'COMPLETED' || data.status === 'FAILED' || data.status === 'CANCELLED') {
          clearInterval(intervalId);
          if (resultContainer) {
            resultContainer.style.display = 'block';
          }
          if (typeof onCompleteCallback === 'function') {
            onCompleteCallback(data);
          } else {
            // Reload page to display full completed state and experiment results
            window.location.reload();
          }
        }
      })
      .catch(function (err) {
        console.warn('Status poll warning:', err);
      });
  }, 1500);
}

function getBadgeClass(status) {
  switch (status) {
    case 'COMPLETED':
    case 'SUCCEEDED':
    case 'VALID':
      return 'badge-success';
    case 'RUNNING':
      return 'badge-primary';
    case 'QUEUED':
      return 'badge-warning';
    case 'FAILED':
    case 'INVALID':
      return 'badge-danger';
    case 'CANCELLED':
    default:
      return 'badge-neutral';
  }
}
