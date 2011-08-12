" Generic vim options that I like
set nocompatible
set hls
set cindent
set expandtab
set softtabstop=4
set tabstop=4
set shiftwidth=4
set ignorecase
set smartcase
set laststatus=2
set shortmess+=T
syntax on

" Add local "vimfiles" directory for file type detection
set runtimepath=vimfiles,$VIMRUNTIME

" Get correct delimiter to use for Java classpath
if has("win32") || has("win16") || has("win64")
   let delim=';'
else
   let delim=':'
endif

" Setup :mak command. It likes spaces to be escaped:
let runIde='java\ -cp\ classes' . delim . 'c:/apps/sql-workbench/drivers/sfdc-kjs-driver-2.jar' . delim . '.\ com.fidelma.salesforce.ide.SalesfarceIDE\ '
execute ":set makeprg=" . runIde . "%"

" Define the error format we use
set errorformat=%f>%l:%c:%t:%n:%m

" Setup other commands, that do not want spaces to be escaped:
let runIde=substitute(runIde, "\\", "", "g")


" Setup more-or-less friendly UTF-8 fields fonts
" (there are still issues with more exotic characters though)
set guifont=courier_new:h10
set enc=utf-8

" Try to make .page files use HTML syntax for highlighting
" Does not work. Oh well.
au BufRead, BufNewFile *.page set syntax=html


" Setup Quick Fix window
autocmd QuickFixCmdPost [^l]* nested if ! empty(getqflist())   | copen 3 | endif
autocmd QuickFixCmdPost    l* nested if ! empty(getloclist(0)) | lwindow 3 | endif


" Keyboard mappings. These are:
"
" F1  - Save the current file to Salesforce. Runs tests if it's a test class
" F2  - 'Drill down' to the current token, splitting the window on the way
" F3  - View the debug log
" F4  - Help make the debug log easier to read
" F5  - View the table information for the current object 

" F8  - Toggle viewing of the class outline

" F9  - Disable a test method (must be on the 'testMethod' declaration)
" F10 - Enable a test method (must be on the 'testMethod' declaration)
" F11 - Disable all test methods
" F12 - Enable all test methods

" ,d  - Download the latest version of the currently edited file
" ,da - Download the latest version of all files
" ,uf - Force a save (upload) of the current file, even though
"       it is out of sync with the version on the server
" ,tag- Retag the source

map <F1> :mak<CR>                                " save/test current file
map <F2> :stag <c-r>=expand("<cword>")<cr><cr>   " Split to tag of current word
map <F3> :b debug.log<CR>                        " Goto the debug log
map <F4> :g!/USER_DEBUG/d<CR>                    " Remove crap from debug log
map <F5> :update<CR>:!start c:\progra~1\intern~1\iexplore.exe "http://sfwb.fronde.info/localist/sandbox-schema/tables/<cword>.html"<CR>

nnoremap <silent> <F8> :TlistToggle<CR>

map <F9> :s,static testMethod void,static /*testMethod*/ void,<CR>   " Disable test method
map <F10> :s,static /\*testMethod\*/ void,static testMethod void,<CR> " Enable test method
map <F11> :%s,static testMethod void,static /*testMethod*/ void,<CR>   " Disable test methods
map <F12> :%s,static /\*testMethod\*/ void,static testMethod void,<CR> " Enable test methods

execute "map ,d :!" . runIde . " -download %<CR>"
execute "map ,da :!" . runIde . " -downloadall<CR>"
execute "map ,uf :!" . runIde . " -force %<CR>"
execute "map ,tag :!ant tag<CR>"
