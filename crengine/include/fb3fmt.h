#ifndef FB3FMT_H
#define FB3FMT_H

#include "../include/crsetup.h"
#include "../include/lvtinydom.h"

bool DetectFb3Format( LVStreamRef stream );
ldomDocument * Fb3GetDescDoc( LVStreamRef stream);
bool ImportFb3Document( LVStreamRef stream, ldomDocument * doc, LVDocViewCallback * progressCallback, CacheLoadingCallback * formatCallback );
LVStreamRef GetFb3Coverpage(LVContainerRef arc);

#endif // FB3FMT_H
