export interface MovieListItem {
    id: string;
    title: string;
    year?: number;
    director?: string;
    rating?: number;
    numVotes?: number;
    genres?: string[];
    stars?: StarSummary[];
}

export interface StarSummary {
    id: string;
    name: string;
}

export interface MovieDetail {
    id: string;
    title: string;
    year?: number;
    director?: string;
    rating?: number;
    numVotes?: number;
    genres?: GenreInfo[];
    stars?: StarInfo[];
}

export interface GenreInfo {
    id: number;
    name: string;
}

export interface StarInfo {
    id: string;
    name: string;
    birthYear?: number;
}
