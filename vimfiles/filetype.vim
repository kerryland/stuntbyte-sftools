" my filetype file
" if exists("did_load_filetypes")
"   finish
" endif
augroup filetypedetect
  au! BufRead,BufNewFile *.cls		setfiletype java
  au! BufRead,BufNewFile *.trigger		setfiletype java
augroup END

