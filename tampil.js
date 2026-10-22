(function(){
  var pintu=[
    "https://cdn.jsdelivr.net/gh/adiytharpansa/youtubetest@main/video-dj.json",
    "https://fastly.jsdelivr.net/gh/adiytharpansa/youtubetest@main/video-dj.json",
    "https://gcore.jsdelivr.net/gh/adiytharpansa/youtubetest@main/video-dj.json"
  ];
  var app=document.getElementById("app");
  function gagal(){ app.innerHTML='<p class="muat">Gagal memuat. Coba muat ulang halaman.</p>'; }
  function tampil(d){
    app.innerHTML="";
    var card=document.createElement("section"); card.className="card";
    var h=document.createElement("h2"); h.textContent=d.judul; card.appendChild(h);
    var wrap=document.createElement("div"); wrap.className="video";
    var f=document.createElement("iframe");
    f.src=d.tautan_putar; f.title=d.judul; f.allowFullscreen=true;
    f.setAttribute("allow","accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share");
    f.loading="lazy";
    wrap.appendChild(f); card.appendChild(wrap);
    var p=document.createElement("p");
    var a=document.createElement("a"); a.href=d.tautan_tonton; a.textContent="Tonton di YouTube";
    p.appendChild(a); card.appendChild(p);
    app.appendChild(card);
  }
  function coba(i){
    if(i>=pintu.length){ gagal(); return; }
    fetch(pintu[i]).then(function(r){ if(!r.ok) throw 0; return r.json(); })
      .then(function(d){ if(d && d.tautan_putar) tampil(d); else coba(i+1); })
      .catch(function(){ coba(i+1); });
  }
  coba(0);
})();
