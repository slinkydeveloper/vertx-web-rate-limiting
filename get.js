import http from "k6/http";

export let options = {
    stages: [
        { duration: "1m", target: 50 },
        { duration: "20s", target: 100 },
        { duration: "40s", target: 150},
        { duration: "1m", target: 150 }
    ]
}

export default function() {
    http.get("http://localhost:4000/hello");
};