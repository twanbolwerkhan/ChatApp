import {getAudioUrl} from "google-tts-api";
import Axios from "axios";
import fs from 'fs';
import {randomUUID} from "crypto";
import {getSounds, insertSound, updateFavorite} from "../dataAccess/soundDAO";

class SoundController {
    get(username:string) {
        return getSounds(username);
    }

    save(name: string, path: string, user: string) {
        insertSound(name, path, user, 0);
    }

    setFavorite(name: string, user: string, favorite: boolean) {
        updateFavorite(name, user, favorite);
    }

    async createTTS(text: string): Promise<string> {
        try {
            const url = getAudioUrl(text);
            const res = await Axios.get(url, {
                responseType: "arraybuffer"
            });
            const buf = Buffer.from(res.data, 'binary');
            const fileName = `${randomUUID()}.mp3`;
            fs.writeFileSync(`uploads/${fileName}`, buf);
            return fileName;
        } catch (e) {
            throw e;
        }
    }


}

const controller = new SoundController();

export default controller;
