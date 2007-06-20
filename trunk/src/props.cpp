/** \file props.cpp
    \brief properties container

    CoolReader Engine

    (c) Vadim Lopatin, 2000-2007
    This source code is distributed under the terms of
    GNU General Public License
    See LICENSE file for details
*/

#include "../include/props.h"

//============================================================================
// CRPropContainer declarations
//============================================================================

class CRPropItem
{
private:
    lString8 _name;
    lString16 _value;
public:
    CRPropItem( const char * name, const lString16 value )
    : _name(name), _value(value)
    { }
    CRPropItem( const CRPropItem& v )
    : _name( v._name )
    , _value( v._value )
    { }
    CRPropItem & operator = ( const CRPropItem& v )
    {
      _name = v._name;
      _value = v._value;
    }
    const char * getName() const { return _name.c_str(); }
    const lString16 & getValue() const { return _value; }
    void setValue(const lString16 &v) { _value = v; }
};

class CRPropContainer : public CRPropAccessor
{
    friend class CRPropSubContainer;
private:
    LVPtrVector<CRPropItem> _list;
protected:
    lInt64 _revision;
    bool findItem( const char * name, int nameoffset, int start, int end, int & pos );
    bool findItem( const char * name, int & pos );
    void clear( int start, int end );
public:
    /// clear all items
    virtual void clear();
    /// returns property path in root container
    virtual const lString8 & getPath() const;
    /// returns property item count in container
    virtual int getCount() const;
    /// returns property name by index
    virtual const char * getName( int index ) const;
    /// returns property value by index
    virtual const lString16 & getValue( int index ) const;
    /// sets property value by index
    virtual void setValue( int index, const lString16 &value );
    /// get string property by name, returns false if not found
    virtual bool getString( const char * propName, lString16 &result ) const;
    /// set string property by name
    virtual void setString( const char * propName, const lString16 &value );
    /// get subpath container
    virtual CRPropRef getSubProps( const char * path );
    /// constructor
    CRPropContainer();
    /// virtual destructor
    virtual ~CRPropContainer();
};

//============================================================================
// CRPropAccessor methods
//============================================================================

lString16 CRPropAccessor::getStringDef( const char * propName, const char * defValue ) const
{
    lString16 value;
    if ( !getString( propName, value ) )
        return lString16( defValue );
    else
        return value;
}

bool CRPropAccessor::getInt( const char * propName, int &result ) const
{
    lString16 value;
    if ( !getString( propName, value ) )
        return false;
    return value.atoi(result);
}

int CRPropAccessor::getIntDef( const char * propName, int defValue ) const
{
    int v = 0;
    if ( !getInt( propName, v ) )
        return defValue;
    else
        return v;
}

void CRPropAccessor::setInt( const char * propName, int value )
{
    setString( propName, lString16::itoa( value ) );
}

bool CRPropAccessor::getBool( const char * propName, bool &result ) const
{
    lString16 value;
    if ( !getString( propName, value ) )
        return false;
    if ( value == L"true" || value == L"TRUE" || value == L"yes" || value == L"YES" || value == L"1" ) {
        result = true;
        return true;
    }
    if ( value == L"false" || value == L"FALSE" || value == L"no" || value == L"NO" || value == L"0" ) {
        result = true;
        return true;
    }
    return false;
}

bool CRPropAccessor::getBoolDef( const char * propName, bool defValue ) const
{
    bool v = 0;
    if ( !getBool( propName, v ) )
        return defValue;
    else
        return v;
}

void CRPropAccessor::setBool( const char * propName, bool value )
{
    setString( propName, lString16( value ? L"true" : L"false" ) );
}

bool CRPropAccessor::getInt64( const char * propName, lInt64 &result ) const
{
    lString16 value;
    if ( !getString( propName, value ) )
        return false;
    return value.atoi(result);
}

lInt64 CRPropAccessor::getInt64Def( const char * propName, lInt64 defValue ) const
{
    lInt64 v = 0;
    if ( !getInt64( propName, v ) )
        return defValue;
    else
        return v;
}

void CRPropAccessor::setInt64( const char * propName, lInt64 value )
{
    setString( propName, lString16::itoa( value ) );
}

CRPropAccessor::~CRPropAccessor()
{
}

static lString8 addBackslashChars( lString8 str )
{
    int i;
    bool found = false;
    for ( i=0; i<str.length(); i++ ) {
        char ch = str[i];
        if ( ch =='\\' || ch=='\r' || ch=='\n' ) {
            found = true;
            break;
        }
    }
    if ( !found )
        return str;
    lString8 out;
    out.reserve( str.length() + 1 );
    for ( i=0; i<str.length(); i++ ) {
        char ch = str[i];
        switch ( ch ) {
        case '\\':
            out << "\\";
            break;
        case '\r':
            out << "\r";
            break;
        case '\n':
            out << "\n";
            break;
        default:
            out << ch;
        }
    }
    return out;
}

static lString8 removeBackslashChars( lString8 str )
{
    int i;
    bool found = false;
    for ( i=0; i<str.length(); i++ ) {
        char ch = str[i];
        if ( ch =='\\' ) {
            found = true;
            break;
        }
    }
    if ( !found )
        return str;
    lString8 out;
    out.reserve( str.length() + 1 );
    for ( i=0; i<str.length(); i++ ) {
        char ch = str[i];
        if ( ch=='\\' ) {
            ch = str[++i];
            switch ( ch ) {
            case 'r':
                out << '\r';
                break;
            case 'n':
                out << '\n';
                break;
            default:
                out << ch;
            }
        } else {
            out << ch;
        }
    }
    return out;
}

/// read from stream
bool CRPropAccessor::loadFromStream( LVStream * stream )
{
    if ( !stream || stream->GetMode()!=LVOM_READ )
        return false;
    lvsize_t sz = stream->GetSize() - stream->GetPos();
    if ( sz<=0 )
        return false;
    char * buf = new char[sz + 3];
    lvsize_t bytesRead = 0;
    if ( stream->Read( buf, sz, &bytesRead )!=LVERR_OK ) {
        delete buf;
        return false;
    }
    buf[sz] = 0;
    char * p = buf;
    if( buf[0] == 0xEF && buf[1]==0xBB && buf[2]==0xBF )
        p += 3;
    // read lines from buffer
    while (*p) {
        char * elp = p;
        char * eqpos = NULL;
        while ( *elp && !(elp[0]=='\r' && elp[1]=='\n') ) {
            if ( *elp == '=' && eqpos==NULL )
                eqpos = elp;
            elp++;
        }
        if ( eqpos!=NULL && eqpos>p ) {
            lString8 name( p, eqpos-p );
            lString8 value( eqpos+1, elp - eqpos - 1);
            setString( name.c_str(), Utf8ToUnicode(removeBackslashChars(value)) );
        }
        p = *elp ? elp + 2 : elp;
    }
    // cleanup
    delete buf;
    return true;
}

/// save to stream
bool CRPropAccessor::saveToStream( LVStream * stream )
{
    if ( !stream || stream->GetMode()!=LVOM_WRITE )
        return false;
    *stream << "\xEF\xBB\xBF";
    for ( int i=0; i<getCount(); i++ ) {
        *stream << getPath() << getName(i) << "=" << addBackslashChars(UnicodeToUtf8(getValue(i))) << "\r\n";
    }
    return true;
}

//============================================================================
// CRPropContainer methods
//============================================================================

CRPropContainer::CRPropContainer()
: _revision(0)
{
}

/// returns property path in root container
const lString8 & CRPropContainer::getPath() const
{
    return lString8::empty_str;
}

/// returns property item count in container
int CRPropContainer::getCount() const
{
    return _list.length();
}

/// returns property name by index
const char * CRPropContainer::getName( int index ) const
{
    return _list[index]->getName();
}

/// returns property value by index
const lString16 & CRPropContainer::getValue( int index ) const
{
    return _list[index]->getValue();
}

/// sets property value by index
void CRPropContainer::setValue( int index, const lString16 &value )
{
    _list[index]->setValue( value );
}

/// binary search
bool CRPropContainer::findItem( const char * name, int nameoffset, int start, int end, int & pos )
{
    int a = start;
    int b = end;
    while ( a < b ) {
        int c = (a + b) / 2;
        int res = lStr_cmp( name, _list[c]->getName() + nameoffset );
        if ( res == 0 ) {
            pos = c;
            return true;
        } else if ( res<0 ) {
            b = c;
        } else {
            a = c + 1;
        }
    }
    pos = a;
    return false;
}

/// binary search
bool CRPropContainer::findItem( const char * name, int & pos )
{
    return findItem( name, 0, 0, _list.length(), pos );
}

/// get string property by name, returns false if not found
bool CRPropContainer::getString( const char * propName, lString16 &result ) const
{
    int pos = 0;
    if ( !findItem( propName, pos ) )
        return false;
    result = _list[pos]->getValue();
    return true;
}

/// clear all items
void CRPropContainer::clear()
{
    _list.clear();
    _revision++;
}

/// set string property by name
void CRPropContainer::setString( const char * propName, const lString16 &value )
{
    int pos = 0;
    if ( !findItem( propName, pos ) ) {
        _list.insert( pos, new CRPropItem( propName, value ) );
        _revision++;
    } else {
        _list[pos]->setValue( value );
    }
}

/// virtual destructor
CRPropContainer::~CRPropContainer()
{
}

void CRPropContainer::clear( int start, int end )
{
    _list.erase( start, end-start );
    _revision++;
}

//============================================================================
// CRPropSubContainer methods
//============================================================================

class CRPropSubContainer : public CRPropAccessor
{
private:
    CRPropContainer * _root;
    lString8 _path;
    int _start;
    int _end;
    lInt64 _revision;
protected:
    void sync()
    {
        if ( _revision != _root->_revision ) {
            _root->findItem( _path.c_str(), _start );
            _root->findItem( (_path + "\xFF").c_str(), _end );
            _revision = _root->_revision;
        }
    }
public:
    CRPropSubContainer(CRPropContainer * root, lString8 path)
    : _root(root), _path(path), _start(0), _end(0), _revision(0)
    {
        sync();
    }
    /// clear all items
    virtual void clear()
    {
        sync();
        _root->clear( _start, _end );
    }
    /// returns property path in root container
    virtual const lString8 & getPath() const
    {
        return _path;
    }
    /// returns property item count in container
    virtual int getCount() const
    {
        sync();
        return _end - _start;
    }
    /// returns property name by index
    virtual const char * getName( int index ) const
    {
        sync();
        return _root->getName( index + _start ) + _path.length();
    }
    /// returns property value by index
    virtual const lString16 & getValue( int index ) const
    {
        sync();
        return _root->getValue( index + _start );
    }
    /// sets property value by index
    virtual void setValue( int index, const lString16 &value )
    {
        sync();
        _root->setValue( index + _start, value );
    }
    /// get string property by name, returns false if not found
    virtual bool getString( const char * propName, lString16 &result ) const
    {
        sync();
        int pos = 0;
        if ( !_root->findItem( propName, _path.length(), _start, _end, pos ) )
            return false;
        result = _root->getValue( pos );
        return true;
    }
    /// set string property by name
    virtual void setString( const char * propName, const lString16 &value )
    {
        sync();
        int pos = 0;
        if ( !_root->findItem( propName, _path.length(), _start, _end, pos ) ) {
            _root->_list.insert( pos, new CRPropItem( (_path + propName).c_str(), value ) );
            _root->_revision++;
            sync();
        } else {
            _root->_list[pos]->setValue( value );
        }
    }
    /// get subpath container
    virtual CRPropRef getSubProps( const char * path )
    {
        return _root->getSubProps( (_path + path).c_str() );
    }
    /// virtual destructor
    virtual ~CRPropSubContainer()
    {
    }
};

/// get subpath container
CRPropRef CRPropContainer::getSubProps( const char * path )
{
    return CRPropRef(new CRPropSubContainer(this, lString8(path)));
}

/// factory function
CRPropRef LVCreatePropsContainer()
{
    return CRPropRef(new CRPropContainer());
}
