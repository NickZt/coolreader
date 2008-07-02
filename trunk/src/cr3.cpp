#include <wx/wx.h>
#include <wx/mstream.h>
#include <wx/stdpaths.h>


#include <crengine.h>
#include "cr3.h"
#include "wolopt.h"
#include "toc.h"
#include "optdlg.h"
#include "rescont.h"
#include "cr3.xpm"



BEGIN_EVENT_TABLE( cr3Frame, wxFrame )
    EVT_MENU( Menu_File_Quit, cr3Frame::OnQuit )
    EVT_MENU( Menu_File_About, cr3Frame::OnAbout )
    EVT_MENU( wxID_OPEN, cr3Frame::OnFileOpen )
    EVT_MENU( wxID_SAVE, cr3Frame::OnFileSave )
    EVT_MENU( Menu_View_TOC, cr3Frame::OnShowTOC )
    EVT_MENU( Menu_File_Options, cr3Frame::OnShowOptions )
    EVT_MENU( Menu_View_History, cr3Frame::OnShowHistory )
    EVT_MENU( Menu_View_Rotate, cr3Frame::OnRotate )
    

    EVT_MENU_RANGE( 0, 0xFFFF, cr3Frame::OnCommand )
//    EVT_UPDATE_UI_RANGE( 0, 0xFFFF, cr3Frame::OnUpdateUI )
    EVT_COMMAND_SCROLL( Window_Id_Scrollbar, cr3Frame::OnScroll )
    EVT_CLOSE( cr3Frame::OnClose )
    EVT_MOUSEWHEEL( cr3Frame::OnMouseWheel )
    EVT_INIT_DIALOG( cr3Frame::OnInitDialog )
    EVT_LIST_ITEM_ACTIVATED(Window_Id_HistList, cr3Frame::OnHistItemActivated)
    //EVT_SIZE    ( cr3Frame::OnSize )
END_EVENT_TABLE()


BEGIN_EVENT_TABLE( cr3scroll, wxScrollBar )
    EVT_SET_FOCUS( cr3scroll::OnSetFocus )
END_EVENT_TABLE()

IMPLEMENT_APP(cr3app)
    

#define USE_XPM_BITMAPS 1


#include "resources/cr3res.h"


ResourceContainer * resources = NULL;

static lChar16 detectSlash( lString16 path )
{
    for ( unsigned i=0; i<path.length(); i++ )
        if ( path[i]=='\\' || path[i]=='/' )
            return path[i];
#ifdef _WIN32
    return '\\';
#else
    return '/';
#endif
}

lString16 GetConfigFileName()
{
    lString16 cfgdir( wxStandardPaths::Get().GetUserDataDir().c_str() );
    if ( !wxDirExists( cfgdir.c_str() ) )
        ::wxMkdir( wxString( cfgdir.c_str() ) );
    lChar16 slash = detectSlash( cfgdir );
    cfgdir << slash;
    return cfgdir + L"cr3.ini";
}

void cr3Frame::OnUpdateUI( wxUpdateUIEvent& event )
{
}

void cr3Frame::OnClose( wxCloseEvent& event )
{
    SaveOptions();
    _view->CloseDocument();
    Destroy();
}


void cr3Frame::OnHistItemActivated( wxListEvent& event )
{
    long index = event.GetIndex();
    if ( index == 0 && _view->getDocView()->isDocumentOpened()) {
        SetActiveMode( am_book );
        return;
    }
    if ( index>=0 && index<_view->getDocView()->getHistory()->getRecords().length() ) {
        lString16 pathname = _view->getDocView()->getHistory()->getRecords()[index]->getFilePath() + 
            _view->getDocView()->getHistory()->getRecords()[index]->getFileName();
        if ( !pathname.empty() ) {
            Update();
            SetActiveMode( am_book );
            _view->LoadDocument( wxString( pathname.c_str()) );
            UpdateToolbar();
        }
    }
}

void cr3Frame::OnCommand( wxCommandEvent& event )
{
    _view->OnCommand( event );
    switch ( event.GetId() ) {
    case Menu_View_ToggleFullScreen:
        _isFullscreen = !_isFullscreen;
        ShowFullScreen( _isFullscreen );
        break;
    case Menu_View_ZoomIn:
    case Menu_View_ZoomOut:
    case Menu_View_NextPage:
    case Menu_View_PrevPage:
    case Menu_Link_Forward:
    case Menu_Link_Back:
    case Menu_Link_Next:
    case Menu_Link_Prev:
    case Menu_Link_Go:
        break;
    }
}

void cr3Frame::OnMouseWheel(wxMouseEvent& event)
{
    _view->OnMouseWheel(event);
}

void cr3Frame::OnSize(wxSizeEvent& event)
{
    if ( _props->getBoolDef(PROP_WINDOW_SHOW_STATUSBAR, true ) ) {
        wxStatusBar * sb = GetStatusBar();
        if ( sb ) {
            int width, height;
            GetClientSize( &width, &height );
            int sw[3] = {width-200, 100, 100};
            sb->SetStatusWidths( 3, sw );
        }
    }
}


void initHyph(const char * fname)
{
    //HyphMan hyphman;
    //return;

    LVStreamRef stream = LVOpenFileStream( fname, LVOM_READ);
    if (!stream)
    {
        printf("Cannot load hyphenation file %s\n", fname);
        return;
    }
    HyphMan::Open( stream.get() );
}

lString8 readFileToString( const char * fname )
{
    lString8 buf;
    LVStreamRef stream = LVOpenFileStream(fname, LVOM_READ);
    if (!stream)
        return buf;
    int sz = stream->GetSize();
    if (sz>0)
    {
        buf.insert( 0, sz, ' ' );
        stream->Read( buf.modify(), sz, NULL );
    }
    return buf;
}


int cr3app::OnExit()
{
    ShutdownFontManager();
    delete resources;
    HyphMan::Close();
#if LDOM_USE_OWN_MEM_MAN == 1
    ldomFreeStorage();
#endif
    return 0;
}

wxBitmap cr3Frame::getIcon16x16( const lChar16 * name )
{
    lString16 dir;
    if ( _toolbarSize==2 )
        dir = lString16(L"icons/22x22/");
    else if ( _toolbarSize==1 )
        dir = lString16(L"icons/16x16/");
    else
        dir = lString16(L"icons/32x32/");
    wxBitmap icon = resources->GetBitmap( (dir + name + L".png").c_str() );
    if ( icon.IsOk() )
        return icon;
    return wxNullBitmap;
}

bool 
cr3app::OnInit()
{
#if 0
    // test property container unit test
    {
        CRPropRef props = LVCreatePropsContainer();
        props->setString("test.string.values.1", lString16("string value 1"));
        props->setString("test.string.values.2", lString16("string value 2 with extra chars(\\\r\n)"));
        props->setBool("test.string.boolean1", true);
        props->setBool("test.string.boolean2", false);
        props->setString("test.string.more_values.2", lString16("string more values (2)"));
        props->setString("test.string.values.22", lString16("string value 22"));
        props->setInt("test.int.value1", 1 );
        props->setInt("test.int.value2", -2 );
        props->setInt("test.int.value3", 3 );
        props->setInt("test.int.value4", 4 );
        props->setInt64("test.int.big-value1", -42387267 );
        props->setPoint("test.points.1", lvPoint(1,2) );
        props->setPoint("test.points.2", lvPoint(3,4) );
        props->setRect("test.rects.1", lvRect(1,2,3,4) );
        props->setRect("test.rects.2", lvRect(-1,-2,3,4) );
        props->setRect("test.rects.1copy", props->getRectDef("test.rects.1", lvRect()) );
        props->setPoint("test.points.1copy", props->getPointDef("test.points.1", lvPoint()) );
        props->setColor("test.colors.1", 0x012345);
        props->setColor("test.colors.1copy", props->getColorDef("test.colors.1") );
        CRPropRef sub = props->getSubProps("test.string.");
        props->setString("test.results.values.1", sub->getStringDef("values.2"));
        props->setInt("test.results.str-items", sub->getCount());
        props->setString("test.results.item1-value", sub->getValue(1));
        props->setString("test.results.item2-name", Utf8ToUnicode(lString8(sub->getName(2))));
        props->setBool("test.results.compare-chars-eq", sub->getStringDef("values.2")==lString16("string value 2 with extra chars(\\\r\n)") );
        LVStreamRef stream = LVOpenFileStream( "props1.ini", LVOM_WRITE );
        props->saveToStream( stream.get() );
    }
    {
        CRPropRef props = LVCreatePropsContainer();
        LVStreamRef istream = LVOpenFileStream( "props1.ini", LVOM_READ );
        LVStreamRef ostream = LVOpenFileStream( "props2.ini", LVOM_WRITE );
        if ( !istream.isNull() && !ostream.isNull() ) {
            props->loadFromStream( istream.get() );
            props->setBool("add.test.result", props->getIntDef("test.int.value2", 0)==-2 );
            props->saveToStream( ostream.get() );
        }
    }
#endif

#if 0
    //CRLog::setFileLogger( "crengine.log" );
    CRLog::setStdoutLogger();
    CRLog::setLogLevel( CRLog::LL_TRACE );
#endif

    wxImage::AddHandler(new wxPNGHandler);
    resources = new ResourceContainer();

    lString16 appname( argv[0] );
    int lastSlash=-1;
    lChar16 slashChar = '/';
    for ( int p=0; p<(int)appname.length(); p++ ) {
        if ( appname[p]=='\\' ) {
            slashChar = '\\';
            lastSlash = p;
        } else if ( appname[p]=='/' ) {
            slashChar = '/';
            lastSlash=p;
        }
    }

    lString16 appPath;
    if ( lastSlash>=0 )
        appPath = appname.substr( 0, lastSlash+1 );

#if 0
    {
        LVStreamRef stream = LVOpenFileStream( "cr3res.zip", LVOM_WRITE );
        stream->Write( cr3_icons, sizeof(cr3_icons), NULL );
    }
#endif

    if (resources->OpenFromMemory( cr3_icons, sizeof(cr3_icons) )) {
/*
        LVStreamRef testStream = resources.GetStream(L"icons/16x16/signature.png");
        if ( !testStream.isNull() ) {
            int sz = testStream->GetSize();
            lUInt8 * buf = new lUInt8[ sz ];
            testStream->Read(buf, sz, NULL);
            printf("read: %c%c%c\n", buf[0], buf[1], buf[2]);
        }
        wxBitmap icon = resources.GetBitmap(L"icons/16x16/signature.png");
        if ( icon.IsOk() ) {
            printf( "Image opened: %dx%d:%d\n", icon.GetWidth(), icon.GetHeight(), icon.GetDepth() );
        }
*/
    }
    lString16 fontDir = appPath + L"fonts";
    fontDir << slashChar;
    InitFontManager( UnicodeToLocal(fontDir)  );

    // Load font definitions into font manager
    // fonts are in files font1.lbf, font2.lbf, ... font32.lbf
    if (!fontMan->GetFontCount()) {


#if (USE_FREETYPE==1)
        LVContainerRef dir = LVOpenDirectory(fontDir.c_str());
        for ( int i=0; i<dir->GetObjectCount(); i++ ) {
            const LVContainerItemInfo * item = dir->GetObjectInfo(i);
            lString16 fileName = item->GetName();
            lString16 fileNameLower = fileName;
            fileNameLower.lowercase();
            if ( !item->IsContainer() && fileNameLower.length()>4 && lString16(fileNameLower, fileNameLower.length()-4, 4)==L".ttf" ) {
                lString8 fn = UnicodeToLocal(fileName);
                printf("loading font: %s\n", fn.c_str());
                if ( !fontMan->RegisterFont(fn) ) {
                    printf("    failed\n");
                }
            }
        }
        //fontMan->RegisterFont(lString8("arial.ttf"));
#else
        #define MAX_FONT_FILE 128
        for (int i=0; i<MAX_FONT_FILE; i++)
        {
            char fn[1024];
            sprintf( fn, "font%d.lbf", i );
            printf("try load font: %s\n", fn);
            fontMan->RegisterFont( lString8(fn) );
        }
#endif
    }

    // init hyphenation manager
    char hyphfn[1024];
    sprintf(hyphfn, "Russian_EnUS_hyphen_(Alan).pdb" );
    initHyph( (UnicodeToLocal(appPath) + hyphfn).c_str() );

    if (!fontMan->GetFontCount())
    {
        //error
#if (USE_FREETYPE==1)
        printf("Fatal Error: Cannot open font file(s) .ttf \nCannot work without font\n" );
#else
        printf("Fatal Error: Cannot open font file(s) font#.lbf \nCannot work without font\nUse FontConv utility to generate .lbf fonts from TTF\n" );
#endif
        return FALSE;
    }

    printf("%d fonts loaded.\n", fontMan->GetFontCount());

#ifdef _WIN32
    //::GetSystemMetrics()
    RECT wrc;
    SystemParametersInfo( SPI_GETWORKAREA, 0, &wrc, 0 );
    int x = wrc.left;
    int y = wrc.top;
    int cx = wrc.right - wrc.left;
    int cy = wrc.bottom - wrc.top;
#else
    int x = 20;
    int y = 40;
    int cx = wxSystemSettings::GetMetric( wxSYS_SCREEN_X );
    int cy = wxSystemSettings::GetMetric( wxSYS_SCREEN_Y )-40;
#endif
    int scale_x = cx * 256 / 620;
    int scale_y = cy * 256 / 830;
    int scale = scale_x < scale_y ? scale_x : scale_y;
    cx = 610 * scale / 256;
    cy = 830 * scale / 256;
    cr3Frame *frame = new cr3Frame( wxT( "CoolReader " wxT(CR3_VERSION) ), wxPoint(x,y), wxSize(cx,cy), appPath );

    

    frame->Show(TRUE);
    SetTopWindow(frame);
    return TRUE;
} 

void cr3Frame::UpdateToolbar()
{
    if ( _toolbarSize==0 )
        return;
    bool enabled_book = _activeMode==am_book && _view->getDocView()->isDocumentOpened();
    wxToolBar* toolBar = GetToolBar();
    toolBar->EnableTool(wxID_SAVE, enabled_book);
    toolBar->EnableTool(Menu_View_ZoomIn, enabled_book);
    toolBar->EnableTool(Menu_View_ZoomOut, enabled_book);
    toolBar->EnableTool(Menu_View_TOC, enabled_book);
    toolBar->EnableTool(Menu_View_Rotate, enabled_book);
    toolBar->EnableTool(Menu_View_TogglePages, enabled_book);
    toolBar->EnableTool(Menu_View_PrevPage, enabled_book);
    toolBar->EnableTool(Menu_View_NextPage, enabled_book);
}

void cr3Frame::SetActiveMode( active_mode_t mode )
{
    if ( mode==_activeMode )
        return;
    switch (mode) {
    case am_book:
        {
            _sizer->Show(_view, true);
            _sizer->Show(_scrollBar, true);
            _sizer->Show(_hist, false);
            _sizer->Layout();
            //_hist->Show(false);
            //SetSizer( _sizer, false );
            //_view->Show(true);
            //_scrollBar->Show(true);
            _view->SetFocus();
        }
        break;
    case am_history:
        {
            _sizer->Show(_view, false);
            _sizer->Show(_scrollBar, false);
            _sizer->Show(_hist, true);
            _sizer->Layout();

            //_histsizer->Show();
            //_sizer->Hide();
            //_view->Show(false);
            //_scrollBar->Show(false);
            //SetSizer( _histsizer, false );
            //_hist->Show(true);
            //_histsizer->Layout();
            _view->getDocView()->savePosition();
            _hist->SetRecords(_view->getDocView()->getHistory()->getRecords());
            _sizer->Layout();
            _hist->UpdateColumns();
            _hist->SetFocus();
        }
        break;
    default:
        break;
    }
    _activeMode = mode;
    UpdateToolbar();
}

void cr3Frame::SetMenu( bool visible )
{
    bool was_visible = (GetMenuBar()!=NULL);
    if ( was_visible == visible )
        return;
    if ( !visible ) {
        SetMenuBar(NULL);
        return;
    }
    wxMenu *menuFile = new wxMenu;

    menuFile->Append( wxID_OPEN, wxT( "&Open...\tCtrl+O" ) );
    menuFile->Append( Menu_View_History, wxT( "Recent books list\tF4" ) );
    menuFile->Append( wxID_SAVE, wxT( "&Save...\tCtrl+S" ) );
    menuFile->AppendSeparator();
    menuFile->Append( Menu_File_Options, wxT( "&Options...\tF9" ) );
    menuFile->AppendSeparator();
    menuFile->Append( Menu_File_About, wxT( "&About...\tF1" ) );
    menuFile->AppendSeparator();
    menuFile->Append( Menu_File_Quit, wxT( "E&xit\tAlt+X" ) );
    
    wxMenu *menuView = new wxMenu;

    menuView->Append( Menu_View_TOC, wxT( "Table of Contents\tF5" ) );
    menuView->Append( Menu_View_History, wxT( "Recent Books\tF4" ) );
    menuView->Append( Menu_View_Rotate, wxT( "Rotate\tCtrl+R" ) );

    menuView->AppendSeparator();
    menuView->Append( Menu_View_ZoomIn, wxT( "Zoom In" ) );
    menuView->Append( Menu_View_ZoomOut, wxT( "Zoom Out" ) );
    menuView->AppendSeparator();
    menuView->Append( Menu_View_ToggleFullScreen, wxT( "Toggle Fullscreen\tAlt+Enter" ) );
    menuView->Append( Menu_View_TogglePages, wxT( "Toggle Pages/Scroll\tCtrl+P" ) );
    menuView->Append( Menu_View_TogglePageHeader, wxT( "Toggle page heading\tCtrl+H" ) );
    
    wxMenuBar *menuBar = new wxMenuBar;
    menuBar->Append( menuFile, wxT( "&File" ) );
    menuBar->Append( menuView, wxT( "&View" ) );
    
    SetMenuBar( menuBar );
}


void cr3Frame::SetStatus( bool visible )
{
    bool was_visible = (GetStatusBar()!=NULL);
    if ( was_visible == visible )
        return;
    if ( !visible ) {
        SetStatusBar(NULL);
        return;
    }
    CreateStatusBar();
    wxStatusBar * status = GetStatusBar();
    int sw[3] = { -1, 100, 100 };
    //int ss[3] = {wxSB_NORMAL, wxSB_FLAT, wxSB_FLAT};
    status->SetFieldsCount(2, sw);
    //status->SetStatusStyles(3, ss);
    SetStatusText( wxT( "Welcome to CoolReader " wxT(CR3_VERSION) wxT("!") ) );
}


static const long TOOLBAR_STYLE = wxTB_FLAT | wxTB_DOCKABLE; // | wxTB_TEXT // | wxTB_DOCKABLE

void cr3Frame::SetToolbarSize( int size )
{
    //if ( size == _toolbarSize )
    //    return;
    _toolbarSize = size;
    if ( !_toolbarSize ) {
        SetToolBar(NULL);
        return;
    }

    wxToolBar* toolBar = GetToolBar();

    long style = toolBar ? toolBar->GetWindowStyle() : TOOLBAR_STYLE;
    delete toolBar;
    SetToolBar(NULL);
    style &= ~(wxTB_HORIZONTAL | wxTB_VERTICAL | wxTB_BOTTOM | wxTB_RIGHT | wxTB_HORZ_LAYOUT);
    int mode = _props->getIntDef( PROP_WINDOW_TOOLBAR_POSITION, 0 );
    switch ( mode ) {
    case 1:
        style |= wxTB_LEFT;
        break;
    default:
        style |= wxTB_TOP;
        break;
    case 2:
        style |= wxTB_RIGHT;
        break;
    case 3:
        style |= wxTB_BOTTOM;
        break;
    }
    //style |= wxTB_NO_TOOLTIPS;
    toolBar = CreateToolBar(style, wxID_ANY);
    /*
    if ( toolBar == NULL ) {
        toolBar = CreateToolBar();
    } else {
        int ids[] = {
            wxID_OPEN, 
            Menu_View_History,
            wxID_SAVE,
            Menu_View_ZoomIn,
            Menu_View_ZoomOut,
            Menu_View_TOC,
            Menu_View_TogglePages,
            Menu_View_ToggleFullScreen,
            Menu_View_PrevPage,
            Menu_View_NextPage,
            Menu_File_Options,
            Menu_File_About,
            -1
        };
        for ( int i=0; ids[i]!=-1; i++ ) {
            toolBar->DeleteTool(ids[i]);
        }
    }
    */

    wxBitmap fileopenBitmap = getIcon16x16(L"fileopen");

    int w = fileopenBitmap.GetWidth(),
        h = fileopenBitmap.GetHeight();



    toolBar->SetToolBitmapSize(wxSize(w, h));
/*
    toolBar->AddTool(wxID_NEW, _T("New"),
                     toolBarBitmaps[Tool_new], wxNullBitmap, wxITEM_NORMAL,
                     _T("New file"), _T("This is help for new file tool"));
*/
    toolBar->AddTool(wxID_OPEN, _T("Open"),
                     fileopenBitmap,//toolBarBitmaps[Tool_open], 
                     wxNullBitmap, wxITEM_NORMAL,
                     _T("Open file"), _T("This is help for open file tool"));
    toolBar->AddTool(Menu_View_History, _T("History (F5)"),
                     getIcon16x16(L"project_open"),//toolBarBitmaps[Tool_open], 
                     wxNullBitmap, wxITEM_NORMAL,
                     _T("Toggle recent books list"), _T("Toggle recent opened books list"));

    toolBar->AddTool(wxID_SAVE, _T("Save"), 
                     getIcon16x16(L"filesaveas"),//toolBarBitmaps[Tool_save], 
                     wxNullBitmap, wxITEM_NORMAL,
                     _T("Save as..."), _T("Export document"));

    toolBar->AddSeparator();
    toolBar->AddTool(Menu_View_ZoomIn, _T("Zoom In"),
                     getIcon16x16(L"viewmag+"),//toolBarBitmaps[Tool_zoomin], 
                     wxNullBitmap, wxITEM_NORMAL,
                     _T("Zoom in"), _T("Increase font size"));
    toolBar->AddTool(Menu_View_ZoomOut, _T("Zoom Out"),
                     getIcon16x16(L"viewmag-"),//toolBarBitmaps[Tool_zoomout], 
                     wxNullBitmap, wxITEM_NORMAL,
                     _T("Zoom out"), _T("Decrease font size"));
    toolBar->AddTool(Menu_View_Rotate, _T("Rotate (Ctrl+R)"),
                     getIcon16x16(L"rotate_cw"),//toolBarBitmaps[Tool_zoomout],
                     wxNullBitmap, wxITEM_NORMAL,
                     _T("Rotate (Ctrl+R)"), _T("Rotate text clockwise"));
    toolBar->AddSeparator();
    toolBar->AddTool(Menu_View_TOC, _T("Table of Contents (F5)"),
                     getIcon16x16(L"player_playlist"),//toolBarBitmaps[Tool_zoomout],
                     wxNullBitmap, wxITEM_NORMAL,
                     _T("Table of Contents (F5)"), _T("Show Table of Contents window"));
    toolBar->AddTool(Menu_View_TogglePages, _T("Toggle pages (Ctrl+P)"),
                     getIcon16x16(L"view_left_right"),//toolBarBitmaps[Tool_zoomout], 
                     wxNullBitmap, wxITEM_NORMAL,
                     _T("Toggle pages (Ctrl+P)"), _T("Switch pages/scroll mode"));
    toolBar->AddTool(Menu_View_ToggleFullScreen, _T("Fullscreen (Alt+Enter)"),
                     getIcon16x16(L"window_fullscreen"),//toolBarBitmaps[Tool_zoomout], 
                     wxNullBitmap, wxITEM_NORMAL,
                     _T("Fullscreen (Alt+Enter)"), _T("Switch to fullscreen mode"));
    toolBar->AddSeparator();
//Menu_View_ToggleFullScreen
    toolBar->AddTool(Menu_View_PrevPage, _T("Previous page"),
                     getIcon16x16(L"previous"),//toolBarBitmaps[Tool_north], 
                     wxNullBitmap, wxITEM_NORMAL,
                     _T("Previous page"), _T("Go to previous page"));
    toolBar->AddTool(Menu_View_NextPage, _T("Next page"),
                     getIcon16x16(L"next"),//toolBarBitmaps[Tool_south], 
                     wxNullBitmap, wxITEM_NORMAL,
                     _T("Next page"), _T("Go to next page"));

    toolBar->AddSeparator();
    toolBar->AddTool(Menu_File_Options, _T("Options"), //wxID_HELP
                     getIcon16x16(L"configure"),//toolBarBitmaps[Tool_help], 
                     wxNullBitmap, wxITEM_NORMAL,
                     _T("Options (F9)"), _T("Options (F9)"));
    toolBar->AddSeparator();
    toolBar->AddTool(Menu_File_About, _T("Help"), //wxID_HELP
                     getIcon16x16(L"help"),//toolBarBitmaps[Tool_help], 
                     wxNullBitmap, wxITEM_NORMAL,
                     _T("Help"), _T("Help"));

    // after adding the buttons to the toolbar, must call Realize() to reflect
    // the changes
    toolBar->Realize();
}

void cr3Frame::OnInitDialog(wxInitDialogEvent& event)
{
    _scrollBar = new cr3scroll(_view);

    _view->SetScrollBar( _scrollBar );
    _view->Create(this, Window_Id_View,
        wxDefaultPosition, wxDefaultSize, 0, wxT("cr3view"));
    _hist->Create(this, Window_Id_HistList);

    _scrollBar->Create(this, Window_Id_Scrollbar,
        wxDefaultPosition, wxDefaultSize, wxSB_VERTICAL);


    wxIcon icon = wxICON(cr3);
    SetIcon( icon );

    //SetMenu( true );

    //SetStatus( true );

    //SetToolbarSize( 3 );


    //_histsizer = new wxBoxSizer( wxHORIZONTAL );

    _sizer = new wxBoxSizer( wxHORIZONTAL );
    _sizer->Add( _view,
        1,
        wxALL | wxEXPAND,
        0);
    _sizer->Add( _scrollBar,
        0,
        wxALIGN_RIGHT | wxEXPAND,
        0);
    _sizer->Add( _hist,
        1,
        wxALL | wxEXPAND,
        0);

    SetSizer( _sizer );

    _view->SetBackgroundColour( _view->getBackgroundColour() );
    //_scrollBar->Show( true );
    SetBackgroundColour( _view->getBackgroundColour() );

    // stylesheet can be placed to file fb2.css
    // if not found, default stylesheet will be used
    char cssfn[1024];
    sprintf( cssfn, "fb2.css"); //, exedir
    lString8 css = readFileToString( (UnicodeToLocal(_appDir) + cssfn).c_str() );
    if (css.length() > 0)
    {
        printf("Style sheet file found.\n");
        _view->getDocView()->setStyleSheet( css );
    }

    _view->UpdateScrollBar();
    _view->Show( true );

    wxAcceleratorEntry entries[40];
    int a=0;
    entries[a++].Set(wxACCEL_CTRL,  (int) 'O',     wxID_OPEN);
    entries[a++].Set(wxACCEL_CTRL,  (int) 'S',     wxID_SAVE);
    entries[a++].Set(wxACCEL_CTRL,  (int) 'P',     Menu_View_TogglePages);
    entries[a++].Set(wxACCEL_CTRL,  (int) 'H',     Menu_View_TogglePageHeader);
    entries[a++].Set(wxACCEL_CTRL,  (int) 'R',     Menu_View_Rotate);
    entries[a++].Set(wxACCEL_NORMAL,  WXK_F3,      wxID_OPEN);
    entries[a++].Set(wxACCEL_NORMAL,  WXK_F2,      wxID_SAVE);
    entries[a++].Set(wxACCEL_NORMAL,  WXK_UP,      Menu_View_PrevLine);
    entries[a++].Set(wxACCEL_NORMAL,  WXK_DOWN,    Menu_View_NextLine);
    entries[a++].Set(wxACCEL_NORMAL,  WXK_SPACE,    Menu_View_NextLine);
    entries[a++].Set(wxACCEL_NORMAL,  WXK_NUMPAD_ADD,      Menu_View_ZoomIn);
    entries[a++].Set(wxACCEL_NORMAL,  WXK_NUMPAD_SUBTRACT, Menu_View_ZoomOut);
    entries[a++].Set(wxACCEL_NORMAL,  WXK_ADD,      Menu_View_ZoomIn);
    entries[a++].Set(wxACCEL_NORMAL,  WXK_SUBTRACT, Menu_View_ZoomOut);
    entries[a++].Set(wxACCEL_CTRL,    WXK_NUMPAD_ADD,      Menu_View_ZoomIn);
    entries[a++].Set(wxACCEL_CTRL,    WXK_NUMPAD_SUBTRACT, Menu_View_ZoomOut);
    entries[a++].Set(wxACCEL_NORMAL,  (int) '+',     Menu_View_ZoomIn);
    entries[a++].Set(wxACCEL_NORMAL,  (int) '-',     Menu_View_ZoomOut);
    entries[a++].Set(wxACCEL_NORMAL,  (int) '=',     Menu_View_ZoomIn);
    entries[a++].Set(wxACCEL_NORMAL,  WXK_PAGEUP,    Menu_View_PrevPage);
    entries[a++].Set(wxACCEL_NORMAL,  WXK_PAGEDOWN,  Menu_View_NextPage);
    entries[a++].Set(wxACCEL_NORMAL,  WXK_HOME,      Menu_View_Begin);
    entries[a++].Set(wxACCEL_NORMAL,  WXK_END,       Menu_View_End);
    entries[a++].Set(wxACCEL_ALT,     WXK_RETURN,     Menu_View_ToggleFullScreen);
    entries[a++].Set(wxACCEL_NORMAL,  WXK_F5,      Menu_View_TOC);
    entries[a++].Set(wxACCEL_NORMAL,  WXK_F4,      Menu_View_History);

    entries[a++].Set(wxACCEL_NORMAL,  WXK_F9,      Menu_File_Options);
    entries[a++].Set(wxACCEL_NORMAL,  WXK_F12,      Menu_File_Quit);
    entries[a++].Set(wxACCEL_NORMAL,  WXK_BACK,      Menu_Link_Back);
    entries[a++].Set(wxACCEL_SHIFT,   WXK_BACK,      Menu_Link_Forward);
    entries[a++].Set(wxACCEL_NORMAL,  WXK_TAB,      Menu_Link_Next);
    entries[a++].Set(wxACCEL_NORMAL,  WXK_RETURN,      Menu_Link_Go);
    entries[a++].Set(wxACCEL_SHIFT,   WXK_TAB,      Menu_Link_Prev);
    
    wxAcceleratorTable accel(a, entries);
    SetAcceleratorTable(accel);
    //_view->SetAcceleratorTable(accel);


    //toolBar->SetRows(!(toolBar->IsVertical()) ? m_rows : 10 / m_rows);
    int argc = wxGetApp().argc;
    lString16 fnameToOpen;
    if ( argc>1 ) {
        fnameToOpen = lString16( wxGetApp().argv[1] );
    } else if ( _props->getBoolDef(PROP_APP_OPEN_LAST_BOOK, true) ) {
        fnameToOpen = _view->GetLastRecentFileName();
    }

    if ( !fnameToOpen.empty() && fnameToOpen[0]=='\"' )
        fnameToOpen.erase(0, 1);
    if ( !fnameToOpen.empty() && fnameToOpen[fnameToOpen.length()-1]=='\"' )
        fnameToOpen.erase(fnameToOpen.length()-1, 1);
    RestoreOptions();
    if ( !fnameToOpen.empty() ) {
        if ( !_view->LoadDocument( wxString( fnameToOpen.c_str() ) ) )
        {
            printf("cannot open document\n");
        }
        SetActiveMode( am_book );
        UpdateToolbar();
    } else {
        SetActiveMode( am_history );
    }

    //Show();
}


cr3Frame::cr3Frame( const wxString& title, const wxPoint& pos, const wxSize& size, lString16 appDir )
    : wxFrame((wxFrame *)NULL, -1, title, pos, size), _activeMode(am_none), _toolbarSize(false)
{
    //wxStandardPaths::GetUserDataDir();

    _props = LVCreatePropsContainer();



    _appDir = appDir;

    _isFullscreen = false;

    {
        // load options from file
        LVStreamRef stream = LVOpenFileStream( GetConfigFileName().c_str(), LVOM_READ );
        if ( !stream.isNull() )
            _props->loadFromStream(stream.get());
    }

    _view = new cr3view( _props );
    _hist = new HistList();

    InitDialog();
}

void cr3Frame::SaveOptions()
{
    //_props->setHex(PROP_FONT_COLOR, _view->getDocView()->getTextColor() );
    //_props->setHex(PROP_BACKGROUND_COLOR, _view->getDocView()->getBackgroundColor() );
    _props->setInt(PROP_CRENGINE_FONT_SIZE, _view->getDocView()->getFontSize() );
    _props->setBool(PROP_WINDOW_FULLSCREEN, _isFullscreen );
    bool maximized = IsMaximized();
    bool minimized = IsIconized();
    _props->setBool(PROP_WINDOW_MAXIMIZED, maximized );
    _props->setBool(PROP_WINDOW_MINIMIZED, minimized );
    if ( !minimized && !maximized && !_isFullscreen ) {
        wxRect rc = GetRect();
        lvRect lvrc( rc.GetLeft(), rc.GetTop(), rc.GetRight(), rc.GetBottom() );
        _props->setRect(PROP_WINDOW_RECT, lvrc );
    }
    {
        // save options to file
        LVStreamRef stream = LVOpenFileStream( GetConfigFileName().c_str(), LVOM_WRITE );
        if ( !stream.isNull() )
            _props->saveToStream(stream.get());
    }
}

void cr3Frame::RestoreOptions()
{
    if ( !_isFullscreen ) {
        SetMenu( _props->getBoolDef(PROP_WINDOW_SHOW_MENU, true) );
        SetStatus( _props->getBoolDef(PROP_WINDOW_SHOW_STATUSBAR, true) );
        int tb = _props->getIntDef(PROP_WINDOW_TOOLBAR_SIZE, 2);
        if ( tb<0 )
            tb = 0;
        if ( tb>3)
            tb = 3;
        SetToolbarSize( tb );
        lvRect lvrc;
        if ( _props->getRect(PROP_WINDOW_RECT, lvrc ) ) {
            wxRect rc( lvrc.left, lvrc.top, lvrc.width(), lvrc.height() );
            SetSize( rc );
        }
        if ( _props->getBoolDef(PROP_WINDOW_MAXIMIZED) )
            Maximize();
        else if ( _props->getBoolDef(PROP_WINDOW_MINIMIZED) )
            Iconize();
    }
    fontMan->SetAntialiasMode( _props->getIntDef( PROP_FONT_ANTIALIASING, 2 ) );
    _view->getDocView()->setTextColor( _props->getIntDef(PROP_FONT_COLOR, 0x000060 ) );
    _view->getDocView()->setBackgroundColor( _props->getIntDef(PROP_BACKGROUND_COLOR, 0xFFFFE0 ) );
    _view->getDocView()->setFontSize( _props->getIntDef( PROP_CRENGINE_FONT_SIZE, 28 ) );
    _view->SetPageHeaderFlags();

    //_view->SetPageHeaderFlags();

    int mode = _props->getIntDef( PROP_PAGE_VIEW_MODE, 2 );
    _view->getDocView()->setViewMode( mode>0 ? DVM_PAGES : DVM_SCROLL, mode>0 ? mode : -1 );

    if ( _props->getBoolDef(PROP_WINDOW_FULLSCREEN) != _isFullscreen ) {
        _isFullscreen = !_isFullscreen;
        if ( _isFullscreen )
            Show();
        ShowFullScreen( _isFullscreen );
    }
}

void 
cr3Frame::OnQuit( wxCommandEvent& WXUNUSED( event ) )
{
    //SaveOptions();
    Close(TRUE);
}

void
cr3Frame::OnShowHistory( wxCommandEvent& event )
{
    switch ( _activeMode ) {
    case am_book:
        SetActiveMode( am_history );
        break;
    case am_history:
        SetActiveMode( am_book );
        break;
    default:
	break;
    }
}

void cr3Frame::OnOptionsChange( CRPropRef oldprops, CRPropRef newprops, CRPropRef changed )
{
    if ( changed->getCount()>0 ) {
        _props->set( newprops );
        SaveOptions();
        RestoreOptions();
    }
    ///
}

void
cr3Frame::OnShowOptions( wxCommandEvent& event )
{
    CR3OptionsDialog dlg(_props);
    dlg.Create( this, Window_Id_Options );
    if ( dlg.ShowModal() == wxID_OK ) {
        // set options
        dlg.ControlsToProps();
        OnOptionsChange( dlg.getOldProps(), dlg.getNewProps(), dlg.getChangedProps() );
    }
}

void
cr3Frame::OnRotate( wxCommandEvent& event )
{
    _view->Rotate();
    SaveOptions();
}

void
cr3Frame::OnShowTOC( wxCommandEvent& event )
{
    LVTocItem * toc = _view->getDocView()->getToc();
    ldomXPointer pos = _view->getDocView()->getBookmark();
    if ( !toc || !toc->getChildCount() )
        return;
    TocDialog dlg( this, toc, _view->getDocView()->getTitle(), pos );
    if ( dlg.ShowModal() == wxID_OK ) {
        // go to
        LVTocItem * sel = dlg.getSelection();
        if ( sel ) {
            ldomXPointer ptr = sel->getXPointer();
            _view->goToBookmark( ptr );
            Update();
        }
    }
}


void 
cr3Frame::OnFileOpen( wxCommandEvent& WXUNUSED( event ) )
{
    wxFileDialog dlg( this, wxT( "Choose a file to open" ), 
        wxT( "" ),
        wxT( "" ),//const wxString& defaultFile = "", 
        wxT("All supported files|*.fb2;*.txt;*.zip;*.rtf|FictionBook files (*.fb2)|*.fb2|RTF files (*.rtf)|*.rtf|Text files (*.txt)|*.txt|ZIP archieves (*.zip)|*.zip"), //const wxString& wildcard = "*.*", 
        wxFD_OPEN | wxFD_FILE_MUST_EXIST //long style = wxFD_DEFAULT_STYLE, 
        //const wxPoint& pos = wxDefaultPosition, 
        //const wxSize& sz = wxDefaultSize, 
        //const wxString& name = "filedlg"
    );

    if ( dlg.ShowModal() == wxID_OK ) {
        //
        Update();
        SetActiveMode( am_book );
        wxCursor hg( wxCURSOR_WAIT );
        this->SetCursor( hg );
        wxSetCursor( hg );
        _view->getDocView()->savePosition();
        _view->LoadDocument( dlg.GetPath() );
        _view->getDocView()->restorePosition();
        _view->UpdateScrollBar();
        //Invalidate();
        Refresh();
        Update();
        UpdateToolbar();
        wxSetCursor( wxNullCursor );
        this->SetCursor( wxNullCursor );
    }

}

void 
cr3Frame::OnFileSave( wxCommandEvent& WXUNUSED( event ) )
{
    wxFileDialog dlg( this, wxT( "Choose a file to open" ), 
        wxT( "" ),
        wxT( "" ),//const wxString& defaultFile = "", 
        wxT("Wolf EBook files (*.wol)|*.wol"), //const wxString& wildcard = "*.*", 
        wxFD_SAVE | wxFD_OVERWRITE_PROMPT //long style = wxFD_DEFAULT_STYLE, 
        //const wxPoint& pos = wxDefaultPosition, 
        //const wxSize& sz = wxDefaultSize, 
        //const wxString& name = "filedlg"
    );
    WolOptions opts( this );
    if ( dlg.ShowModal() == wxID_OK && opts.ShowModal() == wxID_OK ) {
        //
        //_view->LoadDocument( dlg.GetPath() );
        Update();
        wxCursor hg( wxCURSOR_WAIT );
        this->SetCursor( hg );
        wxSetCursor( hg );
        _view->getDocView()->exportWolFile( dlg.GetPath(), opts.getMode()==0, opts.getLevels() );
        wxSetCursor( wxNullCursor );
        this->SetCursor( wxNullCursor );
    }
}

void 
cr3Frame::OnAbout( wxCommandEvent& WXUNUSED( event ) )
{
    wxMessageBox( wxT( "Cool Reader " wxT(CR3_VERSION) wxT("\n(c) 1998-2007 Vadim Lopatin\nwxWidgets version\n") )
    wxT("\nBased on CREngine library " wxT(CR_ENGINE_VERSION) )
    wxT("\nThird party libraries used:")
    wxT("\nzlib, libpng, libjpeg, freetype2,")
    wxT("\nhyphenation library by Alan")
    wxT("\n")
    wxT("\nThe program is being distributed under the terms of GNU General Public License")
    wxT("\nproject homepage is http://www.coolreader.org/crengine.htm")
    wxT("\nsource codes are available at http://sourceforge.net/projects/crengine"
    ),
            wxT( "About Cool Reader" ), wxOK | wxICON_INFORMATION, this );
}

void
cr3Frame::OnScroll(wxScrollEvent& event)
{
    _view->OnScroll( event );
}

void cr3scroll::OnSetFocus( wxFocusEvent& event )
{
    _view->SetFocus();
}

void cr3view::OnSetFocus( wxFocusEvent& event )
{
    GetParent()->SetFocus();
}

void
cr3Frame::OnKeyDown(wxKeyEvent& event)
{
    if ( _activeMode==am_book )
        _view->OnKeyDown( event );
    else
        event.Skip();
}
