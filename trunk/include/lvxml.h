/** \file lvxml.h
    \brief XML parser

   CoolReader Engine

   (c) Vadim Lopatin, 2000-2006

   This source code is distributed under the terms of
   GNU General Public License.

   See LICENSE file for details.

*/

#ifndef __LVXML_H_INCLUDED__
#define __LVXML_H_INCLUDED__

#include "lvstring.h"
#include "lvstream.h"

#define XML_FLAG_NO_SPACE_TEXT 1

//class LVXMLParser;
class LVFileFormatParser;

/// XML parser callback interface
class LVXMLParserCallback
{
protected:
    LVFileFormatParser * _parser;
public:
    /// returns flags
    virtual lUInt32 GetFlags() { return 0; }
    /// sets flags
    virtual void SetFlags( lUInt32 flags ) { }
    /// called on document encoding definition
    virtual void OnEncoding( const lChar16 * name, const lChar16 * table ) { }
    /// called on parsing start
    virtual void OnStart(LVFileFormatParser * parser) { _parser = parser; }
    /// called on parsing end
    virtual void OnStop() = 0;
    /// called on opening tag
    virtual void OnTagOpen( const lChar16 * nsname, const lChar16 * tagname) = 0;
    /// called on closing
    virtual void OnTagClose( const lChar16 * nsname, const lChar16 * tagname ) = 0;
    /// called on element attribute
    virtual void OnAttribute( const lChar16 * nsname, const lChar16 * attrname, const lChar16 * attrvalue ) = 0;
    /// called on text
    virtual void OnText( const lChar16 * text, int len, 
        lvpos_t fpos, lvsize_t fsize, lUInt32 flags ) = 0;
    /// destructor
    virtual ~LVXMLParserCallback() {}
};

/// don't treat CR/LF and TAB characters as space nor remove duplicate spaces
#define TXTFLG_PRE        1
/// last character of previous text was space
#define TXTFLG_LASTSPACE  2

#define TXTFLG_TRIM                         4
#define TXTFLG_TRIM_ALLOW_START_SPACE       8
#define TXTFLG_TRIM_ALLOW_END_SPACE         16
#define TXTFLG_TRIM_REMOVE_EOL_HYPHENS      32

/// converts XML text: decode character entities, convert space chars
int PreProcessXmlString( lChar16 * str, int len, lUInt32 flags );

#define MAX_PERSISTENT_BUF_SIZE 16384

enum char_encoding_type {
    ce_8bit_cp,
    ce_utf8,
    ce_utf16_be,
    ce_utf16_le,
    ce_utf32_be,
    ce_utf32_le,
};

/// base class for all document format parsers
class LVFileFormatParser
{
public:
    /// returns true if format is recognized by parser
    virtual bool CheckFormat() = 0;
    /// parses input stream
    virtual bool Parse() = 0;
    /// resets parsing, moves to beginning of stream
    virtual void Reset() = 0;
    /// stops parsing in the middle of file, to read header only
    virtual void Stop() = 0;
    /// sets charset by name
    virtual void SetCharset( const lChar16 * name ) = 0;
    /// sets 8-bit charset conversion table (128 items, for codes 128..255)
    virtual void SetCharsetTable( const lChar16 * table ) = 0;
    /// returns 8-bit charset conversion table (128 items, for codes 128..255)
    virtual lChar16 * GetCharsetTable( ) = 0;
    /// changes space mode
    virtual void SetSpaceMode( bool flgTrimSpaces ) { }
    /// returns space mode
    virtual bool GetSpaceMode() { return false; }
    /// virtual destructor
    virtual ~LVFileFormatParser();
};

class LVTextFileBase : public LVFileFormatParser
{
protected:
    LVStreamRef m_stream;
    lUInt8 * m_buf;
    int      m_buf_size;
    lvsize_t m_stream_size;
    int      m_buf_len;
    int      m_buf_pos;
    lvpos_t  m_buf_fpos;
    char_encoding_type m_enc_type;
    lString16 m_txt_buf;
    lString16 m_encoding_name;
    lString16 m_lang_name;
    lChar16 * m_conv_table; // charset conversion table for 8-bit encodings
    bool     m_stopped; // true if Stop() is called

    /// tries to autodetect text encoding
    bool AutodetectEncoding();
    /// reads one character from buffer
    lChar16 ReadChar();
    /// fills buffer, to provide specified number of bytes for read
    bool FillBuffer( int bytesToRead );
    /// seek to specified stream position
    bool Seek( lvpos_t pos, int bytesToPrefetch=0 );
    /// reads specified number of bytes, converts to characters and saves to buffer, returns number of chars read
    int ReadTextBytes( lvpos_t pos, int bytesToRead, lChar16 * buf, int buf_size );
    /// reads specified number of characters and saves to buffer, returns number of chars read
    int ReadTextChars( lvpos_t pos, int charsToRead, lChar16 * buf, int buf_size );
public:
    /// stops parsing in the middle of file, to read header only
    virtual void Stop();
    /// returns true if end of fle is reached, and there is no data left in buffer
    bool Eof() { return m_buf_fpos + m_buf_pos >= m_stream_size; }
    /// reads next text line, tells file position and size of line, sets EOL flag
    lString16 ReadLine( int maxLineSize, lvpos_t & fpos, lvsize_t & fsize, lUInt32 & flags );
    /// returns name of character encoding
    lString16 GetEncodingName() { return m_encoding_name; }
    /// returns name of language
    lString16 GetLangName() { return m_lang_name; }

    // overrides
    /// sets charset by name
    virtual void SetCharset( const lChar16 * name );
    /// sets 8-bit charset conversion table (128 items, for codes 128..255)
    virtual void SetCharsetTable( const lChar16 * table );
    /// returns 8-bit charset conversion table (128 items, for codes 128..255)
    virtual lChar16 * GetCharsetTable( ) { return m_conv_table; }
    /// resets parsing, moves to beginning of stream
    virtual void Reset();

    /// constructor
    LVTextFileBase( LVStreamRef stream );
    /// destructor
    virtual ~LVTextFileBase();
};

/** \brief document text cache

    To read fragments of document text on demand.

*/
class LVXMLTextCache : public LVTextFileBase
{
private:
    struct cache_item 
    {
        cache_item * next;
        lUInt32      pos;
        lUInt32      size;
        lUInt32      flags;
        lString16    text;
        cache_item( lString16 & txt )
            : next(NULL), text(txt)
        {
        }
    };

    cache_item * m_head;
    lUInt32    m_max_itemcount;
    lUInt32    m_max_charcount;

    void cleanOldItems( lUInt32 newItemChars );

    /// adds new item
    void addItem( lString16 & str );

public:
    /// returns true if format is recognized by parser
    virtual bool CheckFormat();
    /// parses input stream
    virtual bool Parse();
    /// constructor
    LVXMLTextCache( LVStreamRef stream, lUInt32 max_itemcount, lUInt32 max_charcount )
        : LVTextFileBase( stream ), m_head(NULL)
        , m_max_itemcount(max_itemcount)
        , m_max_charcount(max_charcount)
    {
    }
    /// destructor
    virtual ~LVXMLTextCache();
    /// reads text from cache or input stream
    lString16 getText( lUInt32 pos, lUInt32 size, lUInt32 flags );
};


class LVTextParser : public LVTextFileBase
{
private:
    LVXMLParserCallback * m_callback;
public:
    /// constructor
    LVTextParser( LVStreamRef stream, LVXMLParserCallback * callback );
    /// descructor
    virtual ~LVTextParser();
    /// returns true if format is recognized by parser
    virtual bool CheckFormat();
    /// parses input stream
    virtual bool Parse();
};

/// XML parser
class LVXMLParser : public LVTextFileBase
{
private:
    LVXMLParserCallback * m_callback;
    bool m_trimspaces;
    int  m_state;
    bool SkipSpaces();
    bool SkipTillChar( char ch );
    bool ReadIdent( lString16 & ns, lString16 & str );
    bool ReadText();
public:
    /// returns true if format is recognized by parser
    virtual bool CheckFormat();
    /// parses input stream
    virtual bool Parse();
    /// sets charset by name
    virtual void SetCharset( const lChar16 * name );
    /// resets parsing, moves to beginning of stream
    virtual void Reset();
    /// constructor
    LVXMLParser( LVStreamRef stream, LVXMLParserCallback * callback );
    /// changes space mode
    virtual void SetSpaceMode( bool flgTrimSpaces );
    /// returns space mode
    bool GetSpaceMode() { return m_trimspaces; }
    /// destructor
    virtual ~LVXMLParser();
};


#endif // __LVXML_H_INCLUDED__
