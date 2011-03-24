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
set cmdheight=2
syntax on


set guifont=courier_new:h10
set enc=utf-8
set makeprg=java\ -cp\ classes;lib/ant-salesforce.jar;.\ SalesfarceIDE\ %
set errorformat=%f>%l:%c:%t:%n:%m

"autocmd BufWritePost *.cls mak
"autocmd BufWritePost *.trigger mak
"autocmd BufWritePost *.page mak

au BufRead, BufNewFile *.cls set syntax=cpp
au BufRead, BufNewFile *.trigger set syntax=cpp
au BufRead, BufNewFile *.page set syntax=html



" Keyboard mappings

map <F1> :mak<CR>                                " save/test current file
map <F2> :stag <c-r>=expand("<cword>")<cr><cr>   " Split to tag of current word
map <F3> :b debug.log<CR>                        " Goto the debug log
map <F4> :g!/USER_DEBUG/d<CR>                    " Remove crap from debug log
map <F5> :update<CR>:!start c:\progra~1\intern~1\iexplore.exe "http://sfwb.fronde.info/localist/sandbox-schema/tables/<cword>.html"<CR>

map <F9> :s,static testMethod void,static /*testMethod*/ void,<CR>   " Disable test method
map <F10> :s,static /\*testMethod\*/ void,static testMethod void,<CR> " Enable test method
map <F11> :%s,static testMethod void,static /*testMethod*/ void,<CR>   " Disable test methods
map <F12> :%s,static /\*testMethod\*/ void,static testMethod void,<CR> " Enable test methods


" map <silent> <C-N> :silent noh<CR> " turn off highlighted search
map ,v :sp ~/_vimrc<cr> " edit my .vimrc file in a split
map ,e :e ~/_vimrc<cr>      " edit my .vimrc file
map ,u :source vimrc<cr> " update the system settings from my vimrc file
"----- write out html file
map ,h :source $VIM/vim71/syntax/2html.vim<cr>:w<cr>:clo<cr>

