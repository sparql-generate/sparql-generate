var fs = require('fs');
const gulp = require('gulp');
const fileinclude = require('gulp-file-include');
var rename = require("gulp-rename");
var MarkdownIt = require('markdown-it'), markdown = new MarkdownIt();
var download = require("gulp-download");

async function copy_resources() {
  return gulp.src('./resources/**/*', { dot: true })
    .pipe(gulp.dest('./public'));
}

async function generate_pages() {
  var date = function() {
    var date = new Date();
    return date.getUTCFullYear() + "-" +
      ("0" + (date.getUTCMonth()+1)).slice(-2) + "-" +
      ("0" + date.getUTCDate()).slice(-2)
  }();

  gulp.src(['html/**.html'])
    .pipe(fileinclude({
      context: {"date": date}
    }))
    .pipe(gulp.dest('./public'));

  fs.readdir('./markdown/', (err, files) => {
    files.forEach(file => {

      if(file.match(/^(.+)\.md$/)==null) {
        return;
      }
      var name = file.match(/^(.+)\.md$/)[1];
      var md = fs.readFileSync("./markdown/" + file, {encoding: "utf-8"});
      var args = md.match(/^(@title ([^\r\n]+)\r?\n)?(.*)$/s)
      var title = args[2] == undefined ? "SPARQL-Generate" : args[2] ;
      var html = markdown.render(args[3]);
      gulp.src(['./main.html'])
        .pipe(fileinclude({
          context: {
            title: title,
            name: name,
            body: html,
            date: date}
        }))
        .pipe(rename({extname:'.html',basename:name}))
        .pipe(gulp.dest('./public'));
    });
  });  
}

async function js() {
  gulp.src(['node_modules/clipboard/dist/clipboard.min.js'])
    .pipe(gulp.dest('./public/js'));

  download(["https://sparql-generate.github.io/sparql-generate-editor/dist/sge.bundled.js", "https://sparql-generate.github.io/sparql-generate-editor/dist/sge.bundled.js.map", "https://perfectkb.github.io/yate/dist/yate.bundled.min.js", "https://buttons.github.io/buttons.js"])
    .pipe(gulp.dest("./public/js"));

  download(["https://sparql-generate.github.io/sparql-generate-editor/dist/sge.min.css", "https://sparql-generate.github.io/sparql-generate-editor/dist/sge.min.css.map", "https://perfectkb.github.io/yate/dist/yate.min.css"])
    .pipe(gulp.dest("./public/css"));
}

async function jquery() {
  gulp.src('node_modules/jquery/dist/jquery.min.js').pipe(gulp.dest('./public/js'));
  gulp.src('node_modules/jquery-ui-dist/jquery-ui.min.css').pipe(gulp.dest('./public/css'));
}

async function boostrap() {
  gulp.src(['node_modules/bootstrap/dist/js/bootstrap.min.js','node_modules/bootstrap/dist/js/bootstrap.min.js.map']).pipe(gulp.dest('./public/js'));
  gulp.src(['node_modules/bootstrap/dist/css/bootstrap.min.css','node_modules/bootstrap/dist/css/bootstrap.min.css.map']).pipe(gulp.dest('./public/css'));
  gulp.src('node_modules/bootswatch/dist/pulse/bootstrap.min.css')
    .pipe(rename({basename: "bootswatch-pulse.min", extname:'.css'}))
    .pipe(gulp.dest('./public/css'));

}


exports.default = gulp.parallel(copy_resources, generate_pages, js, jquery, boostrap);



