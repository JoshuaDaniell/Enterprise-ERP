import React from 'react'
import ReactDOM from 'react-dom/client'
import './index.css'
import App from './App.jsx'
console.log("1. main.jsx is running!");
const rootElement = document.getElementById('root');
if (!rootElement) {
  console.error("CRITICAL ERROR: Could not find <div id='root'> in index.html!");
} else {
  ReactDOM.createRoot(rootElement).render(
    <React.StrictMode>
      <App />
    </React.StrictMode>
  );
}
