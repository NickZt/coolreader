/*
    First version of CR3 for EWL, based on etimetool example by Lunohod
*/
#include <stdio.h>
#include <ctype.h>
#include <string.h>
//#include <Ewl.h>
#include <crengine.h>
#include <crgui.h>
#include <crtrace.h>


bool initHyph(const char * fname)
{
    //HyphMan hyphman;
    //return;

    LVStreamRef stream = LVOpenFileStream( fname, LVOM_READ);
    if (!stream)
    {
        printf("Cannot load hyphenation file %s\n", fname);
        return false;
    }
    return HyphMan::Open( stream.get() );
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

void ShutdownCREngine()
{
    HyphMan::Close();
#if LDOM_USE_OWN_MEM_MAN == 1
    ldomFreeStorage();
#endif
}

#if (USE_FREETYPE==1)
bool getDirectoryFonts( lString16Collection & pathList, lString16 ext, lString16Collection & fonts, bool absPath )
{
    int foundCount = 0;
    lString16 path;
    for ( unsigned di=0; di<pathList.length();di++ ) {
        path = pathList[di];
        LVContainerRef dir = LVOpenDirectory(path.c_str());
        if ( !dir.isNull() ) {
            CRLog::trace("Checking directory %s", UnicodeToUtf8(path).c_str() );
            for ( int i=0; i < dir->GetObjectCount(); i++ ) {
                const LVContainerItemInfo * item = dir->GetObjectInfo(i);
                lString16 fileName = item->GetName();
                lString8 fn = UnicodeToLocal(fileName);
                    //printf(" test(%s) ", fn.c_str() );
                if ( !item->IsContainer() && fileName.length()>4 && lString16(fileName, fileName.length()-4, 4)==ext ) {
                    lString16 fn;
                    if ( absPath ) {
                        fn = path;
                        if ( !fn.empty() && fn[fn.length()-1]!=PATH_SEPARATOR_CHAR)
                            fn << PATH_SEPARATOR_CHAR;
                    }
                    fn << fileName;
                    foundCount++;
                    fonts.add( fn );
                }
            }
        }
    }
    return foundCount > 0;
}
#endif

bool InitCREngine( const char * exename )
{
    lString16 appname( exename );
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

    lString16 fontDir = appPath + L"fonts";
    fontDir << slashChar;
    lString8 fontDir8 = UnicodeToLocal(fontDir);
    const char * fontDir8s = fontDir8.c_str();
    //InitFontManager( fontDir8 );
    InitFontManager( lString8() );

    // Load font definitions into font manager
    // fonts are in files font1.lbf, font2.lbf, ... font32.lbf
    if (!fontMan->GetFontCount()) {


    #if (USE_FREETYPE==1)
        lString16 fontExt = L".ttf";
    #else
        lString16 fontExt = L".lbf";
    #endif
    #if (USE_FREETYPE==1)
        lString16Collection fonts;
        lString16Collection fontDirs;
        fontDirs.add( fontDir );
        static const char * msfonts[] = {
            "arial.ttf", "arialbd.ttf", "ariali.ttf", "arialbi.ttf",
            "cour.ttf", "courbd.ttf", "couri.ttf", "courbi.ttf",
            "times.ttf", "timesbd.ttf", "timesi.ttf", "timesbi.ttf",
            NULL
        };
    #ifdef _LINUX
        fontDirs.add( lString16(L"/usr/local/share/crengine/fonts") );
        fontDirs.add( lString16(L"/usr/local/share/fonts/truetype/freefont") );
        fontDirs.add( lString16(L"/usr/share/crengine/fonts") );
        fontDirs.add( lString16(L"/usr/share/fonts/truetype/freefont") );
        fontDirs.add( lString16(L"/root/fonts/truetype") );
        //fontDirs.add( lString16(L"/usr/share/fonts/truetype/msttcorefonts") );
        for ( int fi=0; msfonts[fi]; fi++ )
            fonts.add( lString16(L"/usr/share/fonts/truetype/msttcorefonts/") + lString16(msfonts[fi]) );
    #endif
        getDirectoryFonts( fontDirs, fontExt, fonts, true );

        // load fonts from file
        CRLog::debug("%d font files found", fonts.length());
        if (!fontMan->GetFontCount()) {
            for ( unsigned fi=0; fi<fonts.length(); fi++ ) {
                lString8 fn = UnicodeToLocal(fonts[fi]);
                CRLog::trace("loading font: %s", fn.c_str());
                if ( !fontMan->RegisterFont(fn) ) {
                    CRLog::trace("    failed\n");
                }
            }
        }
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
    if ( !initHyph( (UnicodeToLocal(appPath) + hyphfn).c_str() ) ) {
#ifdef _LINUX
        initHyph( "/usr/share/crengine/hyph/Russian_EnUS_hyphen_(Alan).pdb" );
#endif
    }

    if (!fontMan->GetFontCount())
    {
        //error
#if (USE_FREETYPE==1)
        printf("Fatal Error: Cannot open font file(s) .ttf \nCannot work without font\n" );
#else
        printf("Fatal Error: Cannot open font file(s) font#.lbf \nCannot work without font\nUse FontConv utility to generate .lbf fonts from TTF\n" );
#endif
        return false;
    }

    printf("%d fonts loaded.\n", fontMan->GetFontCount());

    return true;

}

#define CR_USE_XCB
#ifdef CR_USE_XCB

#include <unistd.h>      /* pause() */

#include <xcb/xcb.h>
extern "C" {
#include <xcb/shm.h>
};
#include <xcb/xcb_aux.h>
#include <xcb/xcb_image.h>
#include <xcb/xcb_keysyms.h>
#define XK_MISCELLANY
#include <X11/keysymdef.h>
#include <sys/ipc.h>
#include <sys/shm.h>

#define XCB_ALL_PLANES ~0

static xcb_connection_t *connection;
static xcb_window_t window;
static xcb_screen_t *screen;


/// WXWidget support: draw to wxImage
class CRXCBScreen : public CRGUIScreenBase
{
    public:
        xcb_connection_t * getXcbConnection() { return connection; }
        xcb_window_t getXcbWindow() { return window; }
        xcb_screen_t * getXcbScreen() { return screen; }
    protected:
        xcb_gcontext_t      gc;
        xcb_gcontext_t      bgcolor;
        unsigned int pal_[4];
        xcb_drawable_t rect;
        xcb_shm_segment_info_t shminfo;
        xcb_image_t *im;
        unsigned int *pal;
        virtual void update( const lvRect & rc, bool full )
        {
            printf("update screen, bpp=%d width=%d, height=%d\n", (int)im->bpp,im->width,im->height);
            int i;
            i = xcb_image_shm_get (connection, window,
                    im, shminfo,
                    0, 0,
                    XCB_ALL_PLANES);
            if (!i) {
                printf("cannot get shm image\n");
                return;
            }
            printf("update screen, bpp=%d\n", (int)im->bpp);

            // pal
            static lUInt32 pal[4] = {0x000000, 0x555555, 0xaaaaaa, 0xffffff };
            switch ( im->bpp ) {
            case 32:
                {
                    for ( int y = rc.top; y<rc.bottom; y++ ) {
                        lUInt8 * src = _front->GetScanLine( y );
                        lUInt32 * dst = (lUInt32 *)(im->data + im->stride * y);
                        //printf("line %d : %08X -> %08X   ", y, src, dst);
                        int shift = 6;
                        for ( int x = 0; x< _width; x++ ) {
                            lUInt8 data = src[x>>2];
                            int pixel = (data>>shift) & 3;
                            lUInt32 color = pal[ pixel ]; // to check valgrind finding
                            dst[x] = color;
                            shift -= 2;
                            if ( shift < 0 )
                                shift = 6;
                        }
                    }
                }
                break;
            case 2:
                {
                    for ( int y = rc.top; y<rc.bottom; y++ ) {
                        lUInt8 * src = _front->GetScanLine( y );
                        lUInt8 * dst = (lUInt8 *)(im->data + im->stride * y);
                        memcpy( dst, src, _width>>2 );
                    }
                }
                break;
            }
            //pContext.image = im;
            printf("updated\n");

            //view()->paint();

            xcb_image_shm_put (connection, window, gc,
                    im, shminfo,
                    0, 0, 0, 0, _width, _height, 0);
            xcb_flush(connection);
        }
    public:
        virtual ~CRXCBScreen()
        {
            if ( connection )
                xcb_disconnect( connection );
        }
        CRXCBScreen( int width, int height )
        :  CRGUIScreenBase( 0, 0, true )
        {
            xcb_screen_iterator_t screen_iter;
            const xcb_setup_t    *setup;
            xcb_generic_event_t  *e;
            xcb_generic_error_t  *error;
            xcb_void_cookie_t     cookie_window;
            xcb_void_cookie_t     cookie_map;
            uint32_t              mask;
            uint32_t              values[2];
            int                   screen_number;
            uint8_t               is_hand = 0;

            /* getting the connection */
            connection = xcb_connect (NULL, &screen_number);
            if (xcb_connection_has_error(connection)) {
                fprintf (stderr, "ERROR: can't connect to an X server\n");
                exit(-1);
            }

            screen = xcb_aux_get_screen (connection, screen_number);
            if ( width <= 0 || width > screen->width_in_pixels )
                width = screen->width_in_pixels;
            if ( height <= 0 || height > screen->height_in_pixels )
                height = screen->height_in_pixels;
            xcb_rectangle_t rect_coord = { 0, 0, width, height};

            gc = xcb_generate_id (connection);
            mask = XCB_GC_FOREGROUND | XCB_GC_GRAPHICS_EXPOSURES;
            values[0] = screen->black_pixel;
            values[1] = 0; /* no graphics exposures */
            xcb_create_gc (connection, gc, screen->root, mask, values);

            bgcolor = xcb_generate_id (connection);
            mask = XCB_GC_FOREGROUND | XCB_GC_GRAPHICS_EXPOSURES;
            values[0] = screen->white_pixel;
            values[1] = 0; /* no graphics exposures */
            xcb_create_gc (connection, bgcolor, screen->root, mask, values);

            /* creating the window */
            window = xcb_generate_id(connection);
            mask =  XCB_CW_BACK_PIXEL | XCB_CW_EVENT_MASK;
            xcb_params_cw_t params_cw;
            params_cw.back_pixel = screen->white_pixel;
            params_cw.event_mask =
                XCB_EVENT_MASK_KEY_RELEASE |
                XCB_EVENT_MASK_BUTTON_PRESS |
                XCB_EVENT_MASK_EXPOSURE |
                XCB_EVENT_MASK_POINTER_MOTION;

            uint8_t depth = xcb_aux_get_depth (connection, screen);
            printf("depth = %d, root depth = %d\n",depth, screen->root_depth);
            xcb_aux_create_window(connection,
                    depth,
                    window, screen->root,
                    0, 0, width, height,
                    0, XCB_WINDOW_CLASS_INPUT_OUTPUT,
                    screen->root_visual,
                    mask, &params_cw);

            rect = xcb_generate_id (connection);
            
            xcb_void_cookie_t cookie;
            cookie = xcb_create_pixmap_checked (connection, depth,
                    rect, window,
                    width, height);
            if (xcb_request_check(connection,cookie)){
                printf("sucks, can't creae pixmap\n");
            }

            xcb_map_window(connection, window);

            xcb_colormap_t    colormap;
            colormap = screen->default_colormap;

            xcb_alloc_color_reply_t *rep;
            rep = xcb_alloc_color_reply (connection, xcb_alloc_color (connection, colormap, 0, 0, 0), NULL);
            pal_[0] = rep->pixel;
            free(rep);
            rep = xcb_alloc_color_reply (connection, xcb_alloc_color (connection, colormap, 0x55<<8, 0x55<<8, 0x55<<8), NULL);
            pal_[1] = rep->pixel;
            free(rep);
            rep = xcb_alloc_color_reply (connection, xcb_alloc_color (connection, colormap, 0xaa<<8, 0xaa<<8, 0xaa<<8), NULL);
            pal_[2] = rep->pixel;
            free(rep);
            rep = xcb_alloc_color_reply (connection, xcb_alloc_color (connection, colormap, 0xff<<8, 0xff<<8, 0xff<<8), NULL);
            pal_[3] = rep->pixel;
            free(rep);

            pal = pal_;

            xcb_shm_query_version_reply_t *rep_shm;

            rep_shm = xcb_shm_query_version_reply (connection,
                    xcb_shm_query_version (connection),
                    NULL);
            if(rep_shm) {
                xcb_image_format_t format;
                int shmctl_status;

                if (rep_shm->shared_pixmaps &&
                        (rep_shm->major_version > 1 || rep_shm->minor_version > 0))
                    format = (xcb_image_format_t)rep_shm->pixmap_format;
                else
                    format = (xcb_image_format_t)0;

                im = xcb_image_create_native (connection, width, height,
                        format, depth, NULL, ~0, NULL);
                assert(im);

                shminfo.shmid = shmget (IPC_PRIVATE,
                        im->stride*im->height,
                        IPC_CREAT | 0777);
                assert(shminfo.shmid != -1);
                shminfo.shmaddr = (uint8_t*)shmat (shminfo.shmid, 0, 0);
                assert(shminfo.shmaddr);
                im->data = shminfo.shmaddr;
                printf("Created image depth=%d bpp=%d stride=%d\n", (int)im->depth, (int)im->bpp, (int)im->stride );

                shminfo.shmseg = xcb_generate_id (connection);
                xcb_shm_attach (connection, shminfo.shmseg,
                        shminfo.shmid, 0);
                shmctl_status = shmctl(shminfo.shmid, IPC_RMID, 0);
                assert(shmctl_status != -1);
                free (rep_shm);

            } else {
                printf("Can't get shm\n");
            }
            _width = width;
            _height = height;
            _canvas = LVRef<LVDrawBuf>( new LVGrayDrawBuf( _width, _height, GRAY_BACKBUFFER_BITS ) );
            _front = LVRef<LVDrawBuf>( new LVGrayDrawBuf( _width, _height, GRAY_BACKBUFFER_BITS ) );

            xcb_flush(connection);
            printf("Created screen %d x %d, depth = %d\n", _width, _height, depth );
        }
};


class CRXCBWindowManager : public CRGUIWindowManager
{
protected:
    xcb_connection_t * _connection;
public:
    CRXCBWindowManager( int dx, int dy )
    : CRGUIWindowManager(NULL)
    {
        CRXCBScreen * s = new CRXCBScreen( dx, dy );
        _screen = s;
        _connection = s->getXcbConnection();
        _ownScreen = true;
    }
    // runs event loop
    virtual int runEventLoop()
    {
        xcb_key_symbols_t * keysyms = xcb_key_symbols_alloc( connection );

        xcb_generic_event_t *event;
        bool stop = false;
        while (!stop && (event = xcb_wait_for_event (connection)) ) {
            switch (event->response_type & ~0x80) {
            case XCB_EXPOSE:
                // draw buffer
                {
                    update(true);
                }
                break;
            case XCB_KEY_RELEASE:
                {
                    xcb_key_press_event_t *release = (xcb_key_press_event_t *)event;
                    xcb_keycode_t key = release->detail;
                    int state = release->state;
                    xcb_keysym_t sym = xcb_key_symbols_get_keysym( keysyms,
                                            key,
                                            xcb_lookup_chars_t); //xcb_lookup_key_sym_t xcb_lookup_chars_t
                    printf("Key released keycode=%d char=%04x\n", (int)key, (int)sym );
                    if ( sym==XK_Escape ) {
                        stop = true;
                        break;
                    }
                    int cmd = 0;
#if 0
                    switch ( sym ) {
                    case '0':
                    case XK_Down:
                        cmd = DCMD_PAGEDOWN;
                        break;
                    case '9':
                    case XK_Up:
                        cmd = DCMD_PAGEUP;
                        break;
                    case '+':
                    case '-':
                        cmd = DCMD_ZOOM_IN;
                        break;
                    case '-':
                        cmd = DCMD_ZOOM_OUT;
                        break;
                    }
#endif
                    if ( cmd ) {
                        onCommand( cmd, 0 );
                    } else {
                        onKeyPressed( sym, state );
                    }
                    //printf("page number = %d\n", main_win->getDocView()->getCurPage());
                    update(true);
                }
                break;
            case XCB_BUTTON_PRESS:
                {
                    xcb_button_press_event_t *press = (xcb_button_press_event_t *)event;
                }
                break;
            default:
                /* Unknown event type, ignore it */
                break;
            }

            free (event);

            // stop loop if all windows are closed
            if ( !getWindowCount() )
                stop = true;

        }

        xcb_key_symbols_free( keysyms );
    }
};

#define MAIN_MENU_COMMANDS_START 200
enum CRMainMenuCmd
{
    MCMD_BEGIN = MAIN_MENU_COMMANDS_START,
    MCMD_QUIT,
    MCMD_MAIN_MENU,
    MCMD_GO_PAGE,
};

class V3DocViewWin : public CRDocViewWindow
{
public:
    V3DocViewWin( CRGUIWindowManager * wm )
    : CRDocViewWindow ( wm )
    {
    }

    void showMainMenu()
    {
        #define MENU_FONT_SIZE 20
        LVFontRef menuFont( fontMan->GetFont( MENU_FONT_SIZE, 600, true, css_ff_sans_serif, lString8("Arial")) );
        CRMenu * menu_win = new CRMenu( _wm, 
            NULL, //CRMenu * parentMenu, 
            1, 
            lString16(L"Main Menu"), 
            LVImageSourceRef(), 
            menuFont, 
            menuFont );
        menu_win->addItem( new CRMenuItem( menu_win, DCMD_BEGIN,
                       lString16(L"Go to first page"),
                       LVImageSourceRef(), 
                       menuFont ) );
        menu_win->addItem( new CRMenuItem( menu_win, MCMD_GO_PAGE,
                       lString16(L"Go to page ..."),
                       LVImageSourceRef(), 
                       menuFont ) );
        menu_win->addItem( new CRMenuItem( menu_win, DCMD_END,
                       lString16(L"Go to last page"),
                       LVImageSourceRef(), 
                       menuFont ) );
        _wm->activateWindow( menu_win );
    }

    /// returns true if command is processed
    virtual bool onCommand( int command, int params )
    {
        switch ( command ) {
        case MCMD_QUIT:
            getWindowManager()->closeAllWindows();
            return true;
        case MCMD_MAIN_MENU:
            showMainMenu();
            return true;
        default:
            // do nothing
            ;
        }
        return CRDocViewWindow::onCommand( command, params );
    }
};

int main(int argc, char **argv)
{
    #if 0
    // memory leak test
    {
        {
            lString8 s;
            s << "bla bla bla";
            lString8 s2("xxxxx");
            s << s2;
            lString8 * array = new lString8[25];
            array[2] = lString8("text1");
            array[6] = lString8("text2");
            array[24] = lString8("text3");
            for ( int k=0; k<10000; k++ )
                array[7] << "123";
            typedef LVRef<int> IntRef;
            delete [] array;
            {
                LVCacheMap <int, IntRef> map( 20 );
                map.set(1, IntRef( new int(3) ));
                map.set(2, IntRef( new int(4) ));
            }
            lString8 buf;
            lStringBuf8<100> proxy( buf );
            for ( int i=0; i<5000; i++ )
                buf << 'A';
        }
        ShutdownCREngine();
        return 0;
    }
    #endif


    if ( !InitCREngine( argv[0] ) ) {
        printf("Cannot init CREngine - exiting\n");
        return 2;
    }

    if ( argc!=2 ) {
        printf("Usage: cr3 <filename_to_open>\n");
        return 3;
    }

    const char * fname = argv[1];

    int res = 0;

    {
        CRXCBWindowManager winman( 600, 700 );
        V3DocViewWin * main_win = new V3DocViewWin( &winman );
        main_win->getDocView()->setBackgroundColor(0xFFFFFF);
        main_win->getDocView()->setTextColor(0x000000);
        main_win->getDocView()->setFontSize( 20 );
        static const int acc_table[] = {
            XK_Escape, 0, MCMD_QUIT, 0,
            XK_Return, 0, MCMD_MAIN_MENU, 0, 
            '0', 0, DCMD_PAGEDOWN, 0,
            XK_Down, 0, DCMD_PAGEDOWN, 0,
            '9', 0, DCMD_PAGEUP, 0,
            XK_Up, 0, DCMD_PAGEUP, 0,
            '+', 0, DCMD_ZOOM_IN, 0,
            '=', 0, DCMD_ZOOM_IN, 0,
            '-', 0, DCMD_ZOOM_OUT, 0,
            '_', 0, DCMD_ZOOM_OUT, 0,
            0
        };
        main_win->setAccelerators( CRGUIAcceleratorTableRef( new CRGUIAcceleratorTable( acc_table ) ) );
        winman.activateWindow( main_win );
        if ( !main_win->getDocView()->LoadDocument(fname) ) {
            printf("Cannot open book file %s\n", fname);
            res = 4;
        } else {
            winman.runEventLoop();
        }
    }
    ShutdownCREngine();

    return res;
}


#endif
