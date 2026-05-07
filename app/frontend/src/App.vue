<!--
  App.vue

  Main root component of the application.
  Responsibilities:
  - Defines the global layout (header + main content)
  - Provides navigation using Vue Router
  - Displays current page via <router-view>
-->

<template>
  <div class="app-shell">
    <!-- Header section (fixed at top) -->
    <header class="site-header">
      <div class="site-header-inner">
        
        <!-- Brand / Logo (redirects to Home page) -->
        <router-link to="/" class="brand">
          V1C PROJET
        </router-link>

        <!-- Navigation menu -->
        <nav class="site-nav">
          <!-- Link to Home page -->
          <router-link to="/" class="nav-link">Home</router-link>
          
          <!-- Link to Gallery page -->
          <router-link to="/gallery" class="nav-link">Gallery</router-link>
        </nav>
      </div>
    </header>

    <!-- Main content area -->
    <main class="site-main">
      <!-- Vue Router will inject the current page component here -->
      <router-view />
    </main>
  </div>
</template>

<style scoped>
/* Root container: ensures full viewport height */
.app-shell {
  min-height: 100vh;
}

/* Header styling (sticky top bar with blur effect) */
.site-header {
  position: sticky;
  top: 0;
  z-index: 50;
  background: rgba(5, 8, 17, 0.72);
  backdrop-filter: blur(14px);
  border-bottom: 1px solid rgba(130, 255, 244, 0.08);
}

/* Inner container to center content and control layout */
.site-header-inner {
  width: min(1320px, calc(100% - 40px));
  margin: 0 auto;
  min-height: 76px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

/* Brand/logo styling */
.brand {
  color: #d8fbff;
  text-decoration: none;
  font-size: 0.95rem;
  font-weight: 700;
  letter-spacing: 0.18em;
  text-transform: uppercase;
  text-shadow: 0 0 18px rgba(0, 255, 231, 0.18);
}

/* Navigation container */
.site-nav {
  display: flex;
  align-items: center;
  gap: 26px;
}

/* Navigation link styling */
.nav-link {
  position: relative;
  color: rgba(208, 240, 255, 0.64);
  text-decoration: none;
  font-size: 0.92rem;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  transition: color 0.2s ease, text-shadow 0.2s ease;
}

/* Hover effect for nav links */
.nav-link:hover {
  color: #ffffff;
  text-shadow: 0 0 12px rgba(0, 255, 231, 0.25);
}

/* Active route styling */
.nav-link.router-link-active {
  color: #9efeff;
  text-shadow: 0 0 18px rgba(0, 255, 231, 0.4);
}

/* Underline effect for active link */
.nav-link.router-link-active::after {
  content: "";
  position: absolute;
  left: 0;
  right: 0;
  bottom: -10px;
  height: 1px;
  background: linear-gradient(90deg, transparent, #68fff1, transparent);
  box-shadow: 0 0 12px rgba(104, 255, 241, 0.45);
}

/* Main content area (takes remaining height below header) */
.site-main {
  min-height: calc(100vh - 76px);
}

/* Responsive adjustments for small screens */
@media (max-width: 700px) {
  .site-header-inner {
    width: min(100%, calc(100% - 24px));
    min-height: 68px;
  }

  .brand {
    font-size: 0.8rem;
    letter-spacing: 0.12em;
  }

  .site-nav {
    gap: 14px;
  }

  .nav-link {
    font-size: 0.8rem;
  }
}
</style>