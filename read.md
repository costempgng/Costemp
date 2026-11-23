# Your first Costemp app

A Costemp app is just a folder with one `index.html` file. No build step, no
dependencies — open it in a browser and it runs.

## Step 1: Create the app

```html
<!-- index.html -->
<!doctype html>
<html lang="en">
  <head>
    <meta charset="utf-8" />
    <title>My first Costemp app</title>
    <style>
      body { font-family: system-ui, sans-serif; display: grid; place-items: center; height: 100vh; margin: 0; }
      button { font-size: 1.5rem; padding: 0.5rem 1.25rem; cursor: pointer; }
    </style>
  </head>
  <body>
    <div>
      <h1 id="count">0</h1>
      <button onclick="count.textContent = Number(count.textContent) + 1">Click me</button>
    </div>
  </body>
</html>
```

## Step 2: Run it

Double-click `index.html`, or serve the folder:

```bash
python3 -m http.server 8000
```

Then open <http://localhost:8000>.

## Step 3: Change something

Edit the heading or the button label and refresh the page. That is the whole
loop: edit, refresh, repeat.

## Next steps

- Add more files (`app.js`, `style.css`) and link them from `index.html`.
- Keep state with `localStorage` so it survives a refresh.
- When you are ready, copy the files into your own project and version them with Git.
