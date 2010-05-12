#ifndef CHMFMT_H
#define CHMFMT_H

#include "../include/crsetup.h"
#include "../include/lvtinydom.h"

#if CHM_SUPPORT_ENABLED==1

bool ImportCHMDocument( LVStreamRef stream, ldomDocument * doc, LVDocViewCallback * progressCallback );


/// opens CHM container
LVContainerRef LVOpenCHMContainer( LVStreamRef stream );

#endif

#endif // CHMFMT_H
