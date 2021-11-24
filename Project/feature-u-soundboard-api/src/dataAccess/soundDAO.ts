import useDb from "./database";

export const insertSound = (name: string, path: string, username: string) => {
    useDb((db) => {
        const stmt = db.prepare("INSERT INTO sounds VALUES (?, ?, ?)");
        stmt.run([name, path, username]);
        stmt.finalize();
    });
}

export const getSounds = (user: string) => {
    return new Promise((res) => {
        useDb((db) => {
            const stmt = db.prepare("SELECT * FROM sounds WHERE user = ?");
            stmt.run([user], (result => {
                // tslint:disable-next-line: no-console
                console.log(result);
            }));
            stmt.finalize();
        });
    });

}
