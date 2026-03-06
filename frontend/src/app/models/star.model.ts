export interface StarListItem {
    id: string;
    name: string;
    birthYear?: number;
    movieCount?: number;
}

export interface StarDetail {
    id: string;
    name: string;
    birthYear?: number;
    movies?: MovieInfo[];
}

export interface MovieInfo {
    id: string;
    title: string;
    year?: number;
    director?: string;
}
