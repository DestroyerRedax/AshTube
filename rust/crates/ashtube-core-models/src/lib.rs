use std::fmt;

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum MediaKind {
    Movie,
    Series,
    Anime,
    Kids,
    Adult,
    Other,
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub enum AdultAccessError {
    AgeNotConfirmed,
    RestrictedOrDisabled,
}

impl fmt::Display for AdultAccessError {
    fn fmt(&self, formatter: &mut fmt::Formatter<'_>) -> fmt::Result {
        match self {
            Self::AgeNotConfirmed => write!(formatter, "Adult age confirmation is required."),
            Self::RestrictedOrDisabled => write!(formatter, "Adult content is restricted or disabled."),
        }
    }
}

impl std::error::Error for AdultAccessError {}

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct CatalogItem {
    pub id: String,
    pub title: String,
    pub kind: MediaKind,
    pub year: Option<u16>,
    pub summary: String,
    pub poster_url: Option<String>,
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct Page<T> {
    pub items: Vec<T>,
    pub page: u32,
    pub has_next_page: bool,
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct Episode {
    pub season: u32,
    pub episode: u32,
    pub title: String,
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct MediaDetails {
    pub item: CatalogItem,
    pub episodes: Vec<Episode>,
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct PlaybackOptions {
    pub media_id: String,
    pub qualities: Vec<String>,
    pub subtitles: Vec<SubtitleTrack>,
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct SubtitleTrack {
    pub language: String,
    pub label: String,
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct HistoryItem {
    pub media_id: String,
    pub title: String,
    pub position_ms: u64,
    pub duration_ms: u64,
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct FavoriteItem {
    pub media_id: String,
    pub title: String,
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub enum CoreError {
    NotFound,
    InvalidRequest(String),
    ProviderDisabled,
    UnauthorizedSource,
    AdultAccessRestricted(AdultAccessError),
}

impl fmt::Display for CoreError {
    fn fmt(&self, formatter: &mut fmt::Formatter<'_>) -> fmt::Result {
        match self {
            Self::NotFound => write!(formatter, "The requested item was not found."),
            Self::InvalidRequest(message) => write!(formatter, "Invalid request: {message}"),
            Self::ProviderDisabled => write!(formatter, "This provider is disabled."),
            Self::UnauthorizedSource => write!(formatter, "This source is not authorized."),
            Self::AdultAccessRestricted(error) => write!(formatter, "Adult access error: {error}"),
        }
    }
}

impl std::error::Error for CoreError {}
