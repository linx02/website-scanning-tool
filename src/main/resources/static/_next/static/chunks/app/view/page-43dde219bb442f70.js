(self.webpackChunk_N_E=self.webpackChunk_N_E||[]).push([[982],{7629:(e,s,l)=>{Promise.resolve().then(l.bind(l,7288))},7288:(e,s,l)=>{"use strict";l.r(s),l.d(s,{default:()=>n});var t=l(5155),a=l(2115);function n(){let[e,s]=(0,a.useState)(null),[l,n]=(0,a.useState)(null);return(0,a.useEffect)(()=>{(async()=>{let e=await fetch("".concat("http://localhost:8080/api","/reports")),l=await e.json();s(l);let t=await fetch("".concat("http://localhost:8080/api","/assets")),a=await t.json();n(a),console.log(l),console.log(a)})()},[]),(0,t.jsxs)("div",{className:"[&>h2]:text-3xl [&>h2]:font-semibold mx-12 my-12 [&>h2]:my-8",children:[(0,t.jsx)("h2",{children:"Not emailed"}),l&&l.map(e=>!e.emailed&&e.scannedBy?(0,t.jsx)(i,{asset:e},e.id):null),(0,t.jsx)("h2",{children:"Not scanned"}),l&&l.map(e=>e.emailed||e.scannedBy?null:(0,t.jsx)(i,{asset:e},e.id)),(0,t.jsx)("h2",{children:"Reports"}),e&&e.map(e=>(0,t.jsxs)("div",{className:"grid grid-rows-1 grid-cols-3 border-b-[1px] border-black py-4",children:[(0,t.jsx)("p",{children:e.asset.domain}),(0,t.jsx)("p",{children:new Date(e.createdAt).toISOString().split("T")[0]}),(0,t.jsx)("p",{className:"text-nowrap truncate ...",children:e.report})]},e.id)),(0,t.jsx)("h2",{children:"Assets"}),l&&l.map(e=>(0,t.jsx)(i,{asset:e}))]})}let i=e=>{var s,l;let{asset:a}=e;return(0,t.jsxs)("div",{className:"grid grid-rows-1 grid-cols-6 border-b-[1px] border-black py-4",children:[(0,t.jsx)("p",{children:a.domain}),(0,t.jsx)("p",{children:new Date(a.createdAt).toISOString().split("T")[0]}),(0,t.jsx)("p",{children:a.emailed?"Emailed":"Not emailed"}),(0,t.jsx)("p",{children:a.scannedBy?"Scanned":"Not Scanned"}),(0,t.jsxs)("p",{children:["Emails: ",(null===(s=a.emails)||void 0===s?void 0:s.length)||0]}),(0,t.jsxs)("p",{children:["Phone numbers: ",(null===(l=a.phones)||void 0===l?void 0:l.length)||0]})]})}}},e=>{var s=s=>e(e.s=s);e.O(0,[441,517,358],()=>s(7629)),_N_E=e.O()}]);