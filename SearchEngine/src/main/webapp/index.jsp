<!DOCTYPE html>
<html>
<head>
    <title>Search Form</title>
</head>
<body style="background-color: black; display: flex; flex-direction: column; justify-content: center; align-items: center; height: 100vh; margin: 0; position: relative;">

<!-- Positioning the image 50px below the top -->
<img src="./SpotLight_bgr.png" alt="Spotlight Background" style="max-width: 300px; position: absolute; top: 50px; align-self: center;">

<!-- Centering the form vertically and horizontally -->
<form action="searchForm" method="GET" id="searchForm" style="display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 15px; margin-top: 150px; /* Increased to account for the image and provide space */">
    <input type="text" name="Query" class="input" style="width: 300px; border: none; outline: none; border-radius: 15px; padding: 1em; background-color: #ccc; box-shadow: inset 2px 5px 10px rgba(0,0,0,0.3); transition: 300ms ease-in-out;"
           onfocus="this.style.backgroundColor='white'; this.style.transform='scale(1.05)'; this.style.boxShadow='13px 13px 100px #969696, -13px -13px 100px #eff2ac';"
           onblur="this.style.backgroundColor='#ccc'; this.style.transform='scale(1); this.style.boxShadow='inset 2px 5px 10px rgba(0,0,0,0.3)';"/>

    <input type="submit" class="my-submit-btn" value="Search" style="width: 6.5em; height: 2.3em; background: black; color: white; border: none; border-radius: 0.625em; font-size: 20px; font-weight: bold; cursor: pointer; transition: all 0.5s; position: relative; overflow: hidden;"
           onmouseover="this.style.color='black'; this.style.background='white';"
           onmouseout="this.style.color='white'; this.style.background='black';"
           onmouseenter="this.style.color='black'; this.style.background='white'; this.nextSibling.style.transform='skewX(-45deg) scale(1, 1)';"
           onmouseleave="this.style.color='white'; this.style.background='black'; this.nextSibling.style.transform='skewX(-45deg) scale(0, 1)';"/>